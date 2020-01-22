package controllers;

import db.AnalyticsHandler;
import db.AnalyticsKey;
import domain.Event;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import services.MessageCorrector;
import util.AppConfig;
import util.MessageHandler;
import util.RequestVerifier;
import util.ResultHelper;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class EventController extends Controller {
    private static final String TYPE_URL_VERIFICATION = "url_verification";
    private static final String TYPE_EVENT_CALLBACK = "event_callback";
    private static final String SUBTYPE_CHANNEL_JOIN = "member_joined_channel";
    private static final String SUBTYPE_MESSAGE = "message";
    private static final String VERIFICATION_CHALLENGE = "challenge";

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

        //TODO: handle help request direct im to slackbot
        return handleUserMessage(messages, event);
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

        return _slackService.postChannelJoin(messages, event).thenApplyAsync(slackResponse ->
            slackResponse.ok ? ok(Json.toJson(slackResponse)) : badRequest(Json.toJson(slackResponse))
        , _ec.current());
    }
}
