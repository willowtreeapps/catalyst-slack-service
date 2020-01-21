package controllers;

import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import util.AppConfig;
import util.MessageHandler;
import util.RequestVerifier;

import javax.inject.Inject;
import java.util.Map;

public class HelpController extends Controller {

    private final AppConfig _config;
    private final MessagesApi _messagesApi;

    @Inject
    public HelpController(AppConfig config, MessagesApi messagesApi) {
        this._config = config;
        this._messagesApi = messagesApi;
    }

    public Result handle(Http.Request httpRequest) {
        var body = httpRequest.body().asFormUrlEncoded();

        if (body == null) {
            return noContent();
        }

        var token = body.get("token");
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));

        if (token == null || token.length != 1 ||
                !RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), token[0])) {
            return badRequest(messages.get("error.request.not.verified"));
        }

        var command = body.get("command");
        if (command == null || command.length != 1 || !command[0].equals("/bias-correct-v2")) {
            return noContent();
        }

        String message = messages.get("message.plugin.info");
        var text = body.get("text");
        if (text == null || text.length != 1) {
            message = messages.get("message.specify.action");
        } else if (!text[0].equals("help")) {
            message = messages.get("message.unsupported.action", text[0]);
        }

        return ok(Json.toJson(Map.of("text", message)));
    }
}