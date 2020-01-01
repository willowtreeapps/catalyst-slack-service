package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.i18n.MessagesApi;
import play.mvc.Controller;

import javax.inject.Inject;

public class BaseController extends Controller {

    protected final Config config;
    protected final MessagesApi messagesApi;

    @Inject
    public BaseController(Config config, MessagesApi messagesApi) {
        this.config = config;
        this.messagesApi = messagesApi;
    }

    protected static String getNodeValue(JsonNode parent, String key) {
        JsonNode node = parent.path(key);
        return (isNodeValid(node)) ? node.textValue() : null;
    }

    protected static boolean isNodeValid(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode();
    }
}
