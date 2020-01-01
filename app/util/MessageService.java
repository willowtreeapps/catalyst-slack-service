package util;

import com.fasterxml.jackson.databind.JsonNode;
import play.i18n.Messages;
import play.libs.Json;

import javax.inject.Inject;
import java.util.Map;

public class MessageService {

    private final Messages messages;

    @Inject
    public MessageService(Messages preferred) {
        messages = preferred;
    }

    public JsonNode error(String messageKey) {
        return toJson("error", messageKey);
    }

    public JsonNode toJson(String key, String messageKey) {
        return Json.toJson(Map.of(key, messages.at(messageKey)));
    }
}
