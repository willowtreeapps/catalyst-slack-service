package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Controller;

public class BaseController extends Controller {

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
