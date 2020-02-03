package org.catalyst.slackservice.util;

import play.i18n.Messages;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

public class MessageHandler {

    private final Messages messages;
    public static final String BTN_CORRECT = "button.correct";
    public static final String BTN_IGNORE = "button.ignore";
    public static final String BTN_LEARN_MORE = "button.learn";
    public static final String BTN_AUTHORIZE = "button.authorize";

    public static final String REQUEST_NOT_VERIFIED = "error.request.not.verified";
    public static final String INVALID_REQUEST = "error.invalid.request";
    public static final String MISSING_TYPE = "error.missing.type";
    public static final String UNSUPPORTED_TYPE = "error.unsupported.type";
    public static final String MISSING_CHALLENGE = "error.missing.challenge";
    public static final String INVALID_EVENT = "error.invalid.event";
    public static final String MISSING_CODE = "error.missing.code";
    public static final String MISSING_USER_ACTION_VALUES = "error.missing.user.action.values";
    public static final String MISSING_INTERACTIVE_MESSAGE_FIELDS = "error.missing.iMessage.fields";

    public static final String PLUGIN_INFO = "message.plugin.info";
    public static final String SPECIFY_ACTION = "message.specify.action";
    public static final String UNSUPPORTED_ACTION = "message.unsupported.action";
    public static final String REPLACED_WITH = "message.replaced.with";
    public static final String LEARN_MORE = "message.learn.more";
    public static final String USER_JOINED = "message.user.joined";
    public static final String PLUGIN_ADDED = "message.plugin.added";
    public static final String FALLBACK = "message.fallback";
    public static final String SUGGESTION = "message.suggestion";
    public static final String TITLE = "message.title";

    public SlackLocale slackLocale;
    @Inject
    public MessageHandler(Messages preferred) {
        slackLocale = new SlackLocale();
        messages = preferred;
    }

    public MessageHandler(MessagesApi messagesApi, SlackLocale slackLocale) {
        this.slackLocale = slackLocale;
        messages = messagesApi.preferred(new ArrayList(Arrays.asList(slackLocale.getLang())));
    }

    public String get(String messageKey, Object... args) {
        return messages.at(messageKey, args);
    }
}
