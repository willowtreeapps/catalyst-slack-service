package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Map;

public class EventController extends BaseController {

    @Inject
    public EventController(Config config) {
        super(config);
    }

    public Result handle(Http.Request request) {
        JsonNode node = request.body().asJson();
        if (!validNode(node)) {
            return badRequest("Invalid Request");
        }

        String requestToken = getValueFromJson(node, "token");
        if (requestToken != null && requestToken.equals("1")) {//SlackSecrets.getInstance().getSlackToken())){//getConfigValue("slack.token"))) {
            String requestType = getValueFromJson(node, "type");
            if (requestType == null) {
                return badRequest("Missing Request Type");
            }

            if (requestType.equals("url_verification")) {
                return handleURLVerification(node);
            }
        }

        return ok();
    }

    private Result handleURLVerification(JsonNode node) {
        String challenge = getValueFromJson(node, "challenge");
        if (challenge == null) {
            return badRequest("Invalid Challenge Request, challenge parameter not found!");
        }

        var response = Map.of("challenge", challenge);
        return ok(Json.toJson(response));
    }
}
