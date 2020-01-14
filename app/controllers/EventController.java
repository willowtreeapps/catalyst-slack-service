package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import db.DbManager;
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
    private final DbManager _db;

    @Inject
    public EventController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                           MessageCorrector biasCorrector, AppService slackService, DbManager dbManager) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
        this._db = dbManager;
    }

    private static CompletionStage<Result> resultBadRequest(MessageHandler messages, String error) {
        return CompletableFuture.completedFuture(badRequest(messages.error(error)));
    }

    private static CompletionStage<Result> resultOk(JsonNode json) {
        return CompletableFuture.completedFuture(ok(json));
    }

    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var error = validateRequest(httpRequest);

        if (error != null) {
            return resultBadRequest(messages, error);
        }

        var eventRequest = httpRequest.body().parseJson(Request.class).get();
        if (eventRequest.type.equals("url_verification")) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals("event_callback")) {
            return handleEventCallback(messages, eventRequest.event);
        }

        return resultBadRequest(messages, "error.unsupported.type");
    }

    private String validateRequest(final Http.Request httpRequest) {
        var request = httpRequest.body().parseJson(Request.class);
        if (request.isEmpty()) {
            return "error.invalid.request";
        }

        var headersExist = RequestVerifier.headersExist(httpRequest);
        if (headersExist && !RequestVerifier.verified(_config.getSigningSecret(), httpRequest)) {
            return "error.request.not.verified";
        }

        // token does not exist in the request or is not equal to SLACK_TOKEN environment variable
        var isTokenInvalid = request.filter(r -> r.token == null || !r.token.equals(_config.getToken())).isPresent();
        if (!headersExist && isTokenInvalid) {
            return "error.invalid.token";
        }

        if (request.filter(r -> r.type == null).isPresent()) {
            return "error.missing.type";
        }

        return null;
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

        //TODO: remove
        System.out.println("event captured --> " + Json.toJson(event));
        var result = resultOk(SUCCESS);
        var userName = event.username;
        var botId = event.botId;

        boolean isBotMessage = botId != null && botId.equals(_config.getBotId()) &&
            userName != null && userName.equals(_config.getBotUserName());

        if (isBotMessage || event.user == null || event.text == null) {
            return result;
        }

        if (event.subtype == null) {
            //TODO: handle help request direct im to slackbot
            result = handleUserMessage(messages, event);
        } else if (event.subtype.equals("channel_join")) {
            result = handleChannelJoin(messages, event);
        }

        return result;
    }

    public CompletionStage<Result> handleUserMessage(final MessageHandler messages, final Event event) {
        _db.updateMessageCounts(event.team, event.channel);
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

        return _slackService.postChannelJoinMessage(messages, event).thenApplyAsync(slackResponse ->
            slackResponse.ok ? ok(Json.toJson(slackResponse)) : badRequest(Json.toJson(slackResponse))
        , _ec.current());
    }
}
