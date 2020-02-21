package org.catalyst.slackservice.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.catalyst.slackservice.db.*;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.SlackResponse;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MessageCorrector;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

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
    private final AnalyticsHandler _db;
    private final TokenHandler _tokendb;

    @Inject
    public EventController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                           MessageCorrector biasCorrector, AppService slackService, AnalyticsHandler db, TokenHandler tokenDb) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
        this._db = db;
        this._tokendb = tokenDb;
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

        try {
            var eventRequest = new ObjectMapper().readValue(requestBodyAsBytes.toArray(), Request.class);
            return RequestHandler.GetHandler(eventRequest, httpRequest, _config).handle(this, httpRequest, messages, eventRequest, logger);
        } catch (IOException e) {
            logger.error("unable to parse event request {}", e.getMessage());
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }
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
        var bot = _tokendb.getBotInfo(event.team);
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
        var key = new AnalyticsKey();
        key.teamId = event.team;
        key.channelId = event.channel;

        _db.incrementMessageCounts(key);
        var correctorResult = _biasCorrector.getCorrection(event.text, messages.slackLocale);

        return correctorResult.thenComposeAsync(correction -> {

            if (correction.isEmpty()) {
                return ResultHelper.ok();
            }

            var tokenKey = new TokenKey();
            tokenKey.teamId = event.team;
            tokenKey.userId = event.user;

            // if user has not authorized, show auth prompt instead of the correction prompt
            var userToken = _tokendb.getUserToken(tokenKey);
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
            _tokendb.deleteTokens(teamId, event.tokens.oauth);
            return noContent();
        }, _ec.current());
    }

    private Result handleSlackResponse(Event event, SlackResponse slackResponse, String errorMessage) {
        var json = Json.toJson(slackResponse);
        if (!slackResponse.ok) {
            logger.error("{}. teamId: {}, userId: {}, response: {}", errorMessage,  event.team, event.user, json);
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

    private enum RequestHandler {
        InvalidRequest { /* Not used in practice */
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return false;
            }
        },
        MissingType {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return ResultHelper.badRequest(messages, MessageHandler.MISSING_TYPE);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return request.type == null;
            }
        },
        MissingChallenge {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return ResultHelper.badRequest(messages, MessageHandler.MISSING_CHALLENGE);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return isInvalidUrlVerification(request);
            }
        },
        InvalidEvent {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return ResultHelper.badRequest(messages, MessageHandler.INVALID_EVENT);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return isInvalidEventCallback(request);
            }
        },
        NotVerified {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                logger.error("Request not verified");
                return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return !RequestVerifier.verified(httpRequest, config.getSigningSecret(), config.getToken(), request.token);
            }
        },
        UrlVerification {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return controller.handleURLVerification(request.challenge);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return request.type.equals(TYPE_URL_VERIFICATION);
            }
        },
        EventCallback {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return controller.handleEventCallback(request.event);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return request.type.equals(TYPE_EVENT_CALLBACK) && !SUBTYPE_TOKENS_REVOKED.equals(request.event.type);
            }
        },
        SubtypeTokensRevoked {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return controller.handleRevokeTokens(request.teamId, request.event);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return request.type.equals(TYPE_EVENT_CALLBACK) && SUBTYPE_TOKENS_REVOKED.equals(request.event.type);
            }
        },
        UnsupportedType {
            @Override
            public CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger) {
                return ResultHelper.badRequest(messages, MessageHandler.UNSUPPORTED_TYPE);
            }

            @Override
            public boolean matches(Request request, Http.Request httpRequest, AppConfig config) {
                return false; /* Fallback type, should never match */
            }
        };

        public abstract CompletionStage<Result> handle(EventController controller, Http.Request httpRequest, MessageHandler messages, Request request, Logger logger);
        public abstract boolean matches(Request request, Http.Request httpRequest, AppConfig config);

        public static RequestHandler GetHandler(Request request, Http.Request httpRequest, AppConfig config) {
            return Arrays.stream(RequestHandler.values())
                    .filter( x -> x.matches(request, httpRequest, config) )
                    .findFirst()
                    .orElse(RequestHandler.UnsupportedType);
        }
    }
}
