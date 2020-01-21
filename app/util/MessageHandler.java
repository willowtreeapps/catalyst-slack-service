package util;

import com.fasterxml.jackson.databind.JsonNode;
import play.i18n.Messages;
import play.libs.Json;

import javax.inject.Inject;
import java.util.Map;

public class MessageHandler {

    private final Messages messages;
    public static final String BTN_CORRECT = "button.correct";
    public static final String BTN_NO = "button.no";
    public static final String BTN_LEARN_MORE = "button.learn";
    public static final String BTN_AUTHORIZE = "button.authorize";

    public static final String REQUEST_NOT_VERIFIED = "error.request.not.verified";
    public static final String INVALID_REQUEST = "error.invalid.request";
    public static final String MISSING_TYPE = "error.missing.type";
    public static final String UNSUPPORTED_TYPE = "error.unsupported.type";
    public static final String MISSING_CHALLENGE = "error.missing.challenge";
    public static final String INVALID_EVENT = "error.invalid.event";
    public static final String MISSING_CODE = "error.missing.code";

    public static final String PLUGIN_INFO = "message.plugin.info";
    public static final String SPECIFY_ACTION = "message.specify.action";
    public static final String UNSUPPORTED_ACTION = "message.unsupported.action";
    public static final String REPLACED_WITH = "message.replaced.with";
    public static final String LEARN_MORE = "message.learn.more";
    public static final String GENDER_BIAS_INFO = "message.gender.bias.info";
    public static final String USER_JOINED = "message.user.joined";
    public static final String PLUGIN_ADDED = "message.plugin.added";
    public static final String FALLBACK = "message.fallback";
    public static final String SUGGESTION = "message.suggestion";
    public static final String TITLE = "message.title";

    // TODO: use tika instead?
    @Inject
    public MessageHandler(Messages preferred) {
        messages = preferred;
    }

    public JsonNode error(String messageKey) {
        return toJson("error", messageKey);
    }

    public JsonNode toJson(String key, String messageKey) {
        return Json.toJson(Map.of(key, messages.at(messageKey)));
    }

    public String get(String messageKey, Object... args) {
        return messages.at(messageKey, args);
    }
}
