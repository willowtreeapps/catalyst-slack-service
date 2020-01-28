package org.catalyst.slackservice.util;

import org.catalyst.slackservice.domain.Action;
import org.catalyst.slackservice.domain.Attachment;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.Message;

import java.util.ArrayList;
import java.util.Arrays;

public class MessageGenerator {
    private final static String BTN_STYLE_PRIMARY = "primary";
    private final static String BTN_STYLE_DANGER = "danger";

    public static Message generateSuggestion(MessageHandler msg, Event event, String correction, String token) {
        var correct = new Action(event.text, msg.get(MessageHandler.BTN_CORRECT), Action.YES, BTN_STYLE_PRIMARY, null);
        var ignore = new Action(event.text, msg.get(MessageHandler.BTN_NO), Action.NO, BTN_STYLE_DANGER, null);
        var learnMore = new Action(event.text, msg.get(MessageHandler.BTN_LEARN_MORE), Action.LEARN_MORE, null, null);
        var actions = new ArrayList<>(Arrays.asList(correct, ignore, learnMore));

        var attachment = new Attachment(msg.get(MessageHandler.FALLBACK), msg.get(MessageHandler.TITLE), event.ts, actions);
        var attachments = new ArrayList(Arrays.asList(attachment));

        var message = new Message();
        message.channel = event.channel;
        message.token = token;
        message.user = event.user;
        message.text = msg.get(MessageHandler.SUGGESTION, correction);
        message.attachments = attachments;
        message.threadTs = event.threadTs;

        return message;
    }

    public static Message generatePluginAddedMessage(MessageHandler msg, Event event, String token, String signinUrl, String learnMoreUrl) {
        var message = generateChannelJoinMessage(msg, event, token, signinUrl, learnMoreUrl);
        var leadText = msg.get(MessageHandler.PLUGIN_ADDED);
        var fullText = msg.get(MessageHandler.GENDER_BIAS_INFO, leadText);
        message.text = fullText;

        return message;
    }

    public static Message generateUserJoinedMessage(MessageHandler msg, Event event, String token, String signinUrl, String learnMoreUrl) {
        var message = generateChannelJoinMessage(msg, event, token, signinUrl, learnMoreUrl);
        var leadText = msg.get(MessageHandler.USER_JOINED);
        var fullText = msg.get(MessageHandler.GENDER_BIAS_INFO, leadText);
        message.text = fullText;
        message.user = event.user;

        return message;
    }

    private static Message generateChannelJoinMessage(MessageHandler msg, Event event, String token, String signinUrl, String learnMoreUrl) {
        var authorize = new Action(null, msg.get(MessageHandler.BTN_AUTHORIZE), Action.YES, BTN_STYLE_PRIMARY, signinUrl);
        var learnMore = new Action(null, msg.get(MessageHandler.BTN_LEARN_MORE), Action.LEARN_MORE, null, learnMoreUrl);
        var actions = new ArrayList(Arrays.asList(authorize, learnMore));

        var attachment = new Attachment(msg.get(MessageHandler.FALLBACK), null, null, actions);
        var attachments = new ArrayList(Arrays.asList(attachment));

        var message = new Message();
        message.channel = event.channel;
        message.token = token;
        message.attachments = attachments;

        return message;
    }
}