package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import domain.RequestAction;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import util.MessageService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class EventController extends BaseController {

    private MessageService messages;

    @Inject
    public EventController(Config config, MessagesApi messagesApi) {
        super(config, messagesApi);
    }

    public Result handle(Http.Request request) {

        messages = new MessageService(this.messagesApi.preferred(request));
        JsonNode node = request.body().asJson();
        Optional<RequestAction> requestAction = request.body().parseJson(RequestAction.class);
//        var requestAction = Json.fromJson(node, RequestAction.class);
        Result error = validateRequest(node);
        if (error != null) {
            return error;
        }

        String requestType = getNodeValue(node, "type");
        if (requestType.equals("url_verification")) {
            return handleURLVerification(node);
        }

        return ok();
    }

    private Result validateRequest(JsonNode node) {
        if (!isNodeValid(node)) {
            return badRequest(messages.error("error.invalid.request"));
        }

        String requestToken = getNodeValue(node, "token");
        if (requestToken == null || !requestToken.equals(config.getString("slack_token"))) {
            return badRequest(messages.error("error.invalid.token"));
        }

        String requestType = getNodeValue(node, "type");
        if (requestType == null) {
            return badRequest(messages.error("error.missing.type"));
        }
        return null;
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param node
     * @return
     */
    private Result handleURLVerification(JsonNode node) {
        String challenge = getNodeValue(node, "challenge");
        if (challenge == null) {
            return badRequest(messages.error("error.invalid.challenge"));
        }

        var response = Map.of("challenge", challenge);
        return ok(Json.toJson(response));
    }
}
