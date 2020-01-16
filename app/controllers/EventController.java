package controllers;

import com.fasterxml.jackson.databind.JsonNode;
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

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EventController extends Controller {

    public static class Request {
        public String token;
        public String challenge;
        public String type;
        public Event event;
    }

    private static final JsonNode SUCCESS = Json.toJson(Map.of("ok", Boolean.valueOf(true)));

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

    private static CompletionStage<Result> resultBadRequest(MessageHandler messages, String error) {
        return CompletableFuture.completedFuture(badRequest(messages.error(error)));
    }

    private static CompletionStage<Result> resultOk(JsonNode json) {
        return CompletableFuture.completedFuture(ok(json));
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
            return resultBadRequest(messages,"error.invalid.request");
        }

        var eventRequest = request.get();

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), eventRequest.token)) {
            return resultBadRequest(messages, "error.request.not.verified");
        }

        if (eventRequest.type == null) {
            return resultBadRequest(messages, "error.missing.type");
        }

        if (eventRequest.type.equals("url_verification")) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals("event_callback")) {
            return handleEventCallback(messages, eventRequest.event);
        }

        return resultBadRequest(messages, "error.unsupported.type");
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param messages
     * @param challenge
     * @return
     */
    private CompletionStage<Result> handleURLVerification(final MessageHandler messages, final String challenge) {
        if (challenge == null) {
            return resultBadRequest(messages, "error.missing.challenge");
        }

        return resultOk(Json.toJson(Map.of("challenge", challenge)));
    }

    /**
     * Event callback is triggered for all subscribed events
     * @param messages
     * @param event
     * @return
     */
    private CompletionStage<Result> handleEventCallback(final MessageHandler messages, final Event event) {
        if (event == null) {
            return resultBadRequest(messages, "error.invalid.event");
        }

        var userName = event.username;
        var botId = event.botId;

        boolean isBotMessage = botId != null && botId.equals(_config.getBotId()) &&
            userName != null && userName.equals(_config.getBotUserName());

        boolean isMessageEvent = event.type != null && event.type.equals("message") && event.text != null;
        boolean isChannelJoinEvent = event.type != null && event.type.equals("member_joined_channel");

        if (isBotMessage || event.user == null || !(isMessageEvent || isChannelJoinEvent)) {
            return resultOk(SUCCESS);
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
                return CompletableFuture.completedFuture(ok(SUCCESS));
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
