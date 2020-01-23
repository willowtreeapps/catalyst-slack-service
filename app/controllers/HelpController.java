package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import util.AppConfig;
import util.MessageHandler;
import util.PayloadHelper;
import util.RequestVerifier;

import javax.inject.Inject;
import java.util.Map;

public class HelpController extends Controller {
    final Logger logger = LoggerFactory.getLogger(HelpController.class);

    private final AppConfig _config;
    private final MessagesApi _messagesApi;

    private final static String BIAS_CORRECT = "/bias-correct-v2";
    private final static String TOKEN = "token";
    private final static String COMMAND = "command";
    private final static String TEXT = "text";
    private final static String HELP = "help";

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


        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var token = PayloadHelper.getFirstArrayValue(body.get(TOKEN));
        if (token.isEmpty()) {
            logger.error("empty token");
            return badRequest(messages.get(MessageHandler.REQUEST_NOT_VERIFIED));
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), token.get())) {
            logger.error("request not verified");
            return badRequest(messages.get(MessageHandler.REQUEST_NOT_VERIFIED));
        }


        var command = PayloadHelper.getFirstArrayValue(body.get(COMMAND));
        var text = PayloadHelper.getFirstArrayValue(body.get(TEXT));

        logger.debug(String.format("command: %s, text: %s", command, text));

        if (command.isEmpty() || !command.get().equals(BIAS_CORRECT)) {
            return noContent();
        }

        var message = messages.get(MessageHandler.PLUGIN_INFO);

        if (text.isEmpty() || text.get().isEmpty()) {
            message = messages.get(MessageHandler.SPECIFY_ACTION);
        } else if (!text.get().equals(HELP)) {
            message = messages.get(MessageHandler.UNSUPPORTED_ACTION, text.get());
        }

        return ok(Json.toJson(Map.of(TEXT, message)));
    }
}