package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.AnalyticsHandler;
import org.catalyst.slackservice.db.AnalyticsKey;
import org.catalyst.slackservice.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MessageCorrector;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MessageHandler;
import org.catalyst.slackservice.util.RequestVerifier;
import org.catalyst.slackservice.util.ResultHelper;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class EventController extends Controller {
    final Logger logger = LoggerFactory.getLogger(EventController.class);

    private static final String TYPE_URL_VERIFICATION = "url_verification";
    private static final String TYPE_EVENT_CALLBACK = "event_callback";
    private static final String SUBTYPE_CHANNEL_JOIN = "member_joined_channel";
    private static final String SUBTYPE_MESSAGE = "message";
    private static final String VERIFICATION_CHALLENGE = "challenge";
    private static final String CHANNEL_TYPE_IM = "im";
    private static final String DIRECT_MESSAGE_HELP = "help";

    public static class Request {
        public String token;
        public String challenge;
        public String type;
        public Event event;
    }

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final MessageCorrector _biasCorrector;
    private final AppService _slackService;
    private final HttpExecutionContext _ec;
    private final AnalyticsHandler _db;

    @Inject
    public EventController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                           MessageCorrector biasCorrector, AppService slackService, AnalyticsHandler db) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
        this._db = db;
    }

    /**
     * All event requests will be handled here
     * @param httpRequest
     * @return
     */
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var request = httpRequest.body().parseJson(Request.class);

        if (request.isEmpty()) {
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        var eventRequest = request.get();

        if (eventRequest.type == null) {
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_TYPE);
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), eventRequest.token)) {
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        logger.debug(String.format("incoming event --> %s", Json.toJson(eventRequest).toString()));
        if (eventRequest.type.equals(TYPE_URL_VERIFICATION)) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals(TYPE_EVENT_CALLBACK)) {
            return handleEventCallback(messages, eventRequest.event);
        }

        return ResultHelper.badRequest(messages, MessageHandler.UNSUPPORTED_TYPE);
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param messages
     * @param challenge
     * @return
     */
    private CompletionStage<Result> handleURLVerification(final MessageHandler messages, final String challenge) {
        if (challenge == null) {
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_CHALLENGE);
        }

        return ResultHelper.ok(Json.toJson(Map.of(VERIFICATION_CHALLENGE, challenge)));
    }

    /**
     * Event callback is triggered for all subscribed events
     * @param messages
     * @param event
     * @return
     */
    private CompletionStage<Result> handleEventCallback(final MessageHandler messages, final Event event) {
        if (event == null) {
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_EVENT);
        }

        boolean isBotMessage = _config.getBotId().equals(event.botId) &&
                _config.getBotUserName().equals(event.username);

        boolean isMessageEvent = SUBTYPE_MESSAGE.equals(event.type) && event.text != null;
        boolean isChannelJoinEvent = SUBTYPE_CHANNEL_JOIN.equals(event.type);

        if (isBotMessage || event.user == null || !(isMessageEvent || isChannelJoinEvent)) {
            return ResultHelper.ok();
        }

        if (isChannelJoinEvent) {
            return handleChannelJoin(messages, event);
        }

        var isDirectMessage = CHANNEL_TYPE_IM.equalsIgnoreCase(event.channelType);
        var isBotMentioned = event.text.contains("@" + _config.getBotId());

        if (event.text.toLowerCase().contains(DIRECT_MESSAGE_HELP) && (isDirectMessage || isBotMentioned)) {
            return handleHelpRequest(messages, event);
        }
        return handleUserMessage(messages, event);
    }

    public CompletionStage<Result> handleHelpRequest(final MessageHandler messages, final Event event) {
        return _slackService.postHelpMessage(messages, event).thenApplyAsync(slackResponse -> {
            var response = Json.toJson(slackResponse);
            logger.debug(String.format("help request " + response));
            return slackResponse.ok ? ok(response) : badRequest(response);
        } , _ec.current());
    }

    public CompletionStage<Result> handleUserMessage(final MessageHandler messages, final Event event) {
        var key = new AnalyticsKey();
        key.teamId = event.team;
        key.channelId = event.channel;

        _db.incrementMessageCounts(key);
        var correctorResult = _biasCorrector.getCorrection(event.text);

        return correctorResult.thenComposeAsync(correction -> {

            if (correction.isEmpty()) {
                return ResultHelper.ok();
            }

            return _slackService.postSuggestion(messages, event, correction)
                        .thenApplyAsync(slackResponse ->
                            slackResponse.ok ? ok(Json.toJson(slackResponse)) : badRequest(Json.toJson(slackResponse))
                        , _ec.current());
        }, _ec.current());
    }

    public CompletionStage<Result> handleChannelJoin(final MessageHandler messages, final Event event) {

        return _slackService.postChannelJoin(messages, event).thenApplyAsync(slackResponse -> {
            var json = Json.toJson(slackResponse);
            if (!slackResponse.ok) {
                logger.error(String.format("channel join failed. teamId: %s, userId: %s, response: %s", event.team, event.user, json));
                return badRequest(json);
            }
            return ok(json);
        }, _ec.current());
    }
}