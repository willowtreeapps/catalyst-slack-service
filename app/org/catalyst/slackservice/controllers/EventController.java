package org.catalyst.slackservice.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.catalyst.slackservice.db.*;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.SlackResponse;
import org.catalyst.slackservice.services.*;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MessageHandler;
import org.catalyst.slackservice.util.RequestVerifier;
import org.catalyst.slackservice.util.ResultHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EventController extends Controller {
    final Logger logger = LoggerFactory.getLogger(EventController.class);

    private static final String TYPE_URL_VERIFICATION = "url_verification";
    private static final String TYPE_EVENT_CALLBACK = "event_callback";
    private static final String SUBTYPE_CHANNEL_JOIN = "member_joined_channel";
    private static final String SUBTYPE_MESSAGE = "message";
    private static final String SUBTYPE_TOKENS_REVOKED = "tokens_revoked";
    private static final String VERIFICATION_CHALLENGE = "challenge";
    private static final String CHANNEL_TYPE_IM = "im";
    private static final String DIRECT_MESSAGE_HELP = "help";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        public String token;
        public String challenge;
        public String type;
        @JsonProperty("team_id")
        public String teamId;
        public Event event;
    }

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final MessageCorrector _biasCorrector;
    private final AppService _slackService;
    private final HttpExecutionContext _ec;
    private final AnalyticsService _analyticsService;
    private final TokenHandler _tokenDb;

    @Inject
    public EventController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                           MessageCorrector biasCorrector, AppService slackService, AnalyticsService analyticsService, TokenHandler tokenDb) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
        this._analyticsService = analyticsService;
        this._tokenDb = tokenDb;
    }

    /**
     * All event requests will be handled here
     * @param httpRequest
     * @return
     */
    @BodyParser.Of(BodyParser.Raw.class)
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));

        var requestBodyAsBytes = httpRequest.body().asBytes();
        if (requestBodyAsBytes == null || requestBodyAsBytes.isEmpty()) {
            logger.error("empty event content");
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        // TODO: move outside of controller
        var eventRequest = new Request();
        try {
            eventRequest = new ObjectMapper().readValue(requestBodyAsBytes.toArray(), Request.class);
        } catch (IOException e) {
            logger.error("unable to parse event request {}", e.getMessage());
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        if (eventRequest.type == null) {
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_TYPE);
        }

        if (isInvalidUrlVerification(eventRequest)) {
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_CHALLENGE);
        }

        if (isInvalidEventCallback(eventRequest)) {
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_EVENT);
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), eventRequest.token)) {
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        logger.debug("incoming event --> {}", Json.toJson(eventRequest).toString());

        if (eventRequest.type.equals(TYPE_URL_VERIFICATION)) {
            return handleURLVerification(eventRequest.challenge);
        }

        if (eventRequest.type.equals(TYPE_EVENT_CALLBACK)) {

            var isTokenRevokedEvent = SUBTYPE_TOKENS_REVOKED.equals(eventRequest.event.type);
            if (isTokenRevokedEvent) {
                return handleRevokeTokens(eventRequest.teamId, eventRequest.event);
            }

            return handleEventCallback(eventRequest.event);
        }

        return ResultHelper.badRequest(messages, MessageHandler.UNSUPPORTED_TYPE);
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param challenge
     * @return
     */
    private CompletionStage<Result> handleURLVerification(final String challenge) {
        return ResultHelper.ok(Json.toJson(Map.of(VERIFICATION_CHALLENGE, challenge)));
    }

    /**
     * Event callback is triggered for all subscribed events
     * @param event
     * @return
     */
    private CompletionStage<Result> handleEventCallback(final Event event) {
        var bot = _tokenDb.getBotInfo(event.team);
        if (bot == null || bot.userId == null || bot.token == null) {
            return ResultHelper.ok();
        }

        boolean isMessageEvent = SUBTYPE_MESSAGE.equals(event.type) && event.text != null;
        boolean isChannelJoinEvent = SUBTYPE_CHANNEL_JOIN.equals(event.type);
        boolean isBotMessage = bot.userId.equals(event.user) && isMessageEvent;

        if (isBotMessage || event.user == null || !(isMessageEvent || isChannelJoinEvent)) {
            logger.debug("handleEventCallback exiting isBotMessage: {}, event.user: {}, isMessageEvent:{}, isChannelJoinEvent: {}",
                    isBotMessage, event.user, isMessageEvent, isChannelJoinEvent);
            return ResultHelper.ok();
        }

        var localeResult = _slackService.getConversationLocale(event.channel, bot);

        return localeResult.thenComposeAsync(slackLocale -> {
            var localizedMessages = new MessageHandler(_messagesApi, slackLocale);

            if (isChannelJoinEvent) {
                return handleChannelJoin(localizedMessages, event, bot);
            }

            var isDirectMessage = CHANNEL_TYPE_IM.equalsIgnoreCase(event.channelType);
            var isBotMentioned = event.text.contains("@" + bot.userId);

            if (event.text.toLowerCase().contains(DIRECT_MESSAGE_HELP) && (isDirectMessage || isBotMentioned)) {
                return handleHelpRequest(localizedMessages, event, bot);
            }
            return handleUserMessage(localizedMessages, event, bot);
        });
    }

    private CompletionStage<Result> handleHelpRequest(final MessageHandler messages, final Event event, final Bot bot) {
        return _slackService.postHelpMessage(messages, event, bot).thenApplyAsync(slackResponse -> {
            var response = Json.toJson(slackResponse);
            logger.debug("help request {}", response);
            return slackResponse.ok ? ok(response) : badRequest(response);
        } , _ec.current());
    }

    private CompletionStage<Result> handleUserMessage(final MessageHandler messages, final Event event, final Bot bot) {
        var key = new AnalyticsKey(_config.getTrackingId(), event.team, bot.teamName, event.channel, event.user, messages.slackLocale);
        var correctorResult = _biasCorrector.getCorrection(event.text, messages.slackLocale);
        return correctorResult.thenComposeAsync(correction -> {
            _analyticsService.track(AnalyticsEvent.createMessageEvent(key, correction));
            if (correction.isEmpty()) {
                return ResultHelper.ok();
            }

            var tokenKey = new TokenKey();
            tokenKey.teamId = event.team;
            tokenKey.userId = event.user;

            // if user has not authorized, show auth prompt instead of the correction prompt
            var userToken = _tokenDb.getUserToken(tokenKey);
            if (userToken == null) {
                return _slackService.postReauthMessage(messages, event, bot)
                    .thenApplyAsync(slackResponse -> handleSlackResponse(event, slackResponse, "reauth failed"), _ec.current());
            }

            return _slackService.postSuggestion(messages, event, correction, bot)
                .thenApplyAsync(slackResponse -> handleSlackResponse(event, slackResponse, "postSuggestion failed")
                , _ec.current());
        }, _ec.current());
    }

    private CompletionStage<Result> handleChannelJoin(final MessageHandler messages, final Event event, final Bot bot) {

        return _slackService.postChannelJoin(messages, event, bot).thenApplyAsync(slackResponse ->
            handleSlackResponse(event, slackResponse, "channel join failed")
        , _ec.current());
    }

    private CompletionStage<Result> handleRevokeTokens(final String teamId, final Event event) {
        return CompletableFuture.supplyAsync(() -> {
            _tokenDb.deleteTokens(teamId, event.tokens.oauth);
            return noContent();
        }, _ec.current());
    }

    private Result handleSlackResponse(Event event, SlackResponse slackResponse, String errorMessage) {
        var json = Json.toJson(slackResponse);
        if (!slackResponse.ok) {
            logger.error("{}. teamId: {}, userId: {}", errorMessage,  event.team, event.user);
            return badRequest(json);
        }
        return ok(json);
    }

    private static boolean isInvalidUrlVerification(final Request request) {
        return request.type.equals(TYPE_URL_VERIFICATION) && request.challenge == null;
    }

    private static boolean isInvalidEventCallback(final Request request){
        return request.type.equals(TYPE_EVENT_CALLBACK) && (request.event == null || (!SUBTYPE_TOKENS_REVOKED.equals(request.event.type) && request.event.channel == null));
    }
}
