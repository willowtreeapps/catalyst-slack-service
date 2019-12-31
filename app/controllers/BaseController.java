package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import play.mvc.Controller;

import javax.inject.Inject;

public class BaseController extends Controller {

    private final Config config;

    @Inject
    public BaseController(Config config) {
        this.config = config;
    }

    // refactor!
    protected String getValueFromJson(JsonNode parent, String key) {
        String value = null;
        JsonNode valueNode = parent.path(key);
        if (valueNode != null && !valueNode.isNull() && !valueNode.isMissingNode()) {
            value = valueNode.textValue();
        }
        return value;
    }

    protected boolean validNode(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode();
    }
}
