package controllers;

import com.fasterxml.jackson.databind.JsonNode;
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

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
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

    @Inject
    public EventController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi, MessageCorrector biasCorrector, AppService slackService) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
    }

    private static CompletionStage<Result> resultBadRequest(MessageHandler messages, String error) {
        return CompletableFuture.completedFuture(badRequest(messages.error(error)));
    }

    private static CompletionStage<Result> resultOk(JsonNode json) {
        return CompletableFuture.completedFuture(ok(json));
    }

    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var optionalRequest = httpRequest.body().parseJson(Request.class);
        var error = validateRequest(optionalRequest);

        if (error != null) {
            return resultBadRequest(messages, error);
        }

        var eventRequest = optionalRequest.get();
        if (eventRequest.type.equals("url_verification")) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals("event_callback")) {
            return handleEventCallback(messages, eventRequest.event);
        }

        return resultBadRequest(messages, "error.unsupported.type");
    }

    private String validateRequest(final Optional<Request> request) {
        if (request.isEmpty()) {
            return "error.invalid.request";
        }

        if (request.filter(r -> r.token == null || !r.token.equals(_config.getToken())).isPresent()) {
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

        var userName = event.username;
        var botId = event.botId;

        boolean isBotMessage = botId != null && botId.equals(_config.getBotId()) &&
            userName != null && userName.equals(_config.getBotUserName());

        if (isBotMessage || event.user == null || event.text == null) {
            return resultOk(SUCCESS);
        }

        if (event.subtype == null) {
            //TODO: handle help request direct im to slackbot
            return handleUserMessage(messages, event);
        } else {
            //TODO: handle channel_join
            return resultOk(SUCCESS);
        }
    }

    public CompletionStage<Result> handleUserMessage(final MessageHandler messages, final Event event) {
        CompletionStage<String> correctorResult = _biasCorrector.getCorrection(event.text);
        CompletionStage<Result> slackResult = correctorResult.thenComposeAsync(correction -> {

            if (correction.isEmpty()) {
                return CompletableFuture.completedFuture(ok(SUCCESS));
            } else {
                return _slackService.postSuggestion(messages, event, correction)
                        .thenApplyAsync(slackResponse -> ok(Json.toJson(slackResponse)), _ec.current());
            }
        }, _ec.current());

        return slackResult;
    }
}
