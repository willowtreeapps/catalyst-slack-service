package controllers;

import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class EventController extends BaseController {

    public static class Request {
        public String token;
        public String challenge;
        public String type;
    }

    @Inject
    public EventController(AppConfig config, MessagesApi messagesApi) {
        super(config, messagesApi);
    }

    public Result handle(Http.Request httpRequest) {
        var messages = new MessageHandler(messagesApi.preferred(httpRequest));
        var optionalRequest = httpRequest.body().parseJson(Request.class);
        var error = validateRequest(optionalRequest);

        if (error != null) {
            return badRequest(messages.error(error));
        }

        var eventRequest = optionalRequest.get();
        if (eventRequest.type.equals("url_verification")) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals("event_callback")) {
            return handleEventCallback();
        }

        return badRequest(messages.error("error.unsupported.type"));
    }

    private String validateRequest(final Optional<Request> request) {
        if (request.isEmpty()) {
            return "error.invalid.request";
        }

        if (request.filter(r -> r.token == null || !r.token.equals(config.getToken())).isPresent()) {
            return "error.invalid.token";
        }

        if (request.filter(r -> r.type == null).isPresent()) {
            return "error.missing.type";
        }

        return null;
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param challenge
     * @return
     */
    private Result handleURLVerification(final MessageHandler messages, final String challenge) {
        if (challenge == null) {
            return badRequest(messages.error("error.missing.challenge"));
        }

        return ok(Json.toJson(Map.of("challenge", challenge)));
    }

    private Result handleEventCallback() {
        return ok();
    }
}
