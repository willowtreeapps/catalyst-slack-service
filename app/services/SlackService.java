package services;

import domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class SlackService implements AppService, WSBodyReadables {
    final Logger logger = LoggerFactory.getLogger(SlackService.class);

    private final static String CONTENT_TYPE_JSON = "application/json";
    private final static String HEADER_AUTHORIZATION = "Authorization";
    private final static String QUERY_PARAM_CODE = "code";
    private final static String QUERY_PARAM_ID = "client_id";
    private final static String QUERY_PARAM_SECRET = "client_secret";
    private final static String POST_DELETE_ORIGINAL = "delete_original";
    private final static String BTN_STYLE_PRIMARY = "primary";
    private final static String BTN_STYLE_DANGER = "danger";

    private final WSClient _wsClient;
    private final AppConfig _config;
    private final HttpExecutionContext _ec;

    @Inject
    SlackService(HttpExecutionContext ec, AppConfig config, WSClient wsClient) {
        this._ec = ec;
        this._wsClient = wsClient;
        this._config = config;
    }

    public Message generateSuggestion(MessageHandler msg, Event event, String correction) {
        var correct = new Action(event.text, msg.get(MessageHandler.BTN_CORRECT), Action.YES, BTN_STYLE_PRIMARY, null);
        var ignore = new Action(event.text, msg.get(MessageHandler.BTN_NO), Action.NO, BTN_STYLE_DANGER, null);
        var learnMore = new Action(event.text, msg.get(MessageHandler.BTN_LEARN_MORE), Action.LEARN_MORE, null, null);
        var actions = new ArrayList<>(Arrays.asList(correct, ignore, learnMore));

        var attachment = new Attachment(msg.get(MessageHandler.FALLBACK), msg.get(MessageHandler.TITLE), event.ts, actions);
        var attachments = new ArrayList(Arrays.asList(attachment));

        var message = new Message();
        message.channel = event.channel;
        message.token = _config.getBotOauthToken();
        message.user = event.user;
        message.text = msg.get(MessageHandler.SUGGESTION, correction);
        message.attachments = attachments;

        return message;
    }

    private Message generateChannelJoinMessage(MessageHandler msg, Event event) {
        var authorize = new Action(null, msg.get(MessageHandler.BTN_AUTHORIZE), Action.YES, BTN_STYLE_PRIMARY, _config.getAppSigninUrl());
        var learnMore = new Action(null, msg.get(MessageHandler.BTN_LEARN_MORE), Action.LEARN_MORE, null, _config.getLearnMoreUrl());
        var actions = new ArrayList(Arrays.asList(authorize, learnMore));

        var attachment = new Attachment(msg.get(MessageHandler.FALLBACK), null, null, actions);
        var attachments = new ArrayList(Arrays.asList(attachment));

        var message = new Message();
        message.channel = event.channel;
        message.token = _config.getBotOauthToken();
        message.attachments = attachments;

        return message;
    }

    public Message generatePluginAddedMessage(MessageHandler msg, Event event) {
        var message = generateChannelJoinMessage(msg, event);
        var leadText = msg.get(MessageHandler.PLUGIN_ADDED);
        var fullText = msg.get(MessageHandler.GENDER_BIAS_INFO, leadText);
        message.text = fullText;

        return message;
    }

    public Message generateUserJoinedMessage(MessageHandler msg, Event event) {
        var message = generateChannelJoinMessage(msg, event);
        var leadText = msg.get(MessageHandler.USER_JOINED);
        var fullText = msg.get(MessageHandler.GENDER_BIAS_INFO, leadText);
        message.text = fullText;
        message.user = event.user;

        return message;
    }

    private CompletionStage<SlackResponse> postReply(String url, Message reply, String authToken) {

        var request = _wsClient.url(url).setContentType(CONTENT_TYPE_JSON).
                addHeader(HEADER_AUTHORIZATION, String.format("Bearer %s", authToken));

        var jsonReply = Json.toJson(reply);
        var jsonPromise = request.post(jsonReply);

        return jsonPromise.thenApplyAsync(r -> {
                    var response = r.getBody(json());
                    logger.debug(String.format("\nposting to slack %s --> %s\n response --> %s", url, jsonReply, response));
                    return Json.fromJson(response, SlackResponse.class);
                }
                , _ec.current());
    }

    public CompletionStage<SlackResponse> postSuggestion(final MessageHandler messages, final Event event, final String correction) {
        var botReply = generateSuggestion(messages, event, correction);
        return postReply(_config.getPostEphemeralUrl(), botReply, _config.getBotOauthToken());
    }

    public CompletionStage<SlackResponse> postChannelJoin(final MessageHandler messages, final Event event) {

        String url = _config.getPostEphemeralUrl();
        Message message = generateUserJoinedMessage(messages, event);

        if (_config.getBotId().equals(event.user)) {
            url = _config.getPostMessageUrl();
            message = generatePluginAddedMessage(messages, event);
        }

        return postReply(url, message, _config.getBotOauthToken());
    }

    public CompletionStage<AuthResponse> getAuthorization(String requestCode) {
        var request = _wsClient.url(_config.getOauthUrl()).
                addQueryParameter(QUERY_PARAM_CODE, requestCode).
                addQueryParameter(QUERY_PARAM_ID, _config.getClientId()).
                addQueryParameter(QUERY_PARAM_SECRET, _config.getClientSecret());

        var jsonPromise = request.get();

        return jsonPromise.thenApplyAsync(r -> Json.fromJson(r.getBody(json()), AuthResponse.class), _ec.current());
    }

    public CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage) {
        var message = new Message();
        message.token = _config.getBotOauthToken();
        message.channel = iMessage.channel.id;
        message.text = msg.get(MessageHandler.LEARN_MORE);
        message.user = iMessage.user.id;

        return postReply(_config.getPostEphemeralUrl(), message, _config.getBotOauthToken());
    }

    public CompletionStage<SlackResponse> postReplacement(
            MessageHandler msg, InteractiveMessage iMessage, String correction, String userToken) {

        var originalPost = iMessage.actions.stream().findFirst().get().name;
        var message = new Message();
        message.token = userToken;
        message.channel = iMessage.channel.id;
        message.text = correction;
        message.ts = iMessage.callbackId;

        var url = _config.getUpdateUrl();

        if (userToken == null) {
            message.token = _config.getBotOauthToken();
            message.text = msg.get(MessageHandler.REPLACED_WITH, iMessage.user.name, originalPost, correction);

            url = _config.getPostMessageUrl();
        }

        return postReply(url, message, message.token);
    }

    public CompletionStage<SlackResponse> deleteMessage(InteractiveMessage iMessage) {

        var request = _wsClient.url(iMessage.responseUrl).setContentType(CONTENT_TYPE_JSON);
        var jsonPromise = request.post(Json.toJson(Map.of(POST_DELETE_ORIGINAL, Boolean.valueOf(true))));

        return jsonPromise.thenApplyAsync(r -> Json.fromJson(r.getBody(json()), SlackResponse.class), _ec.current());
    }

    @Override
    public CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event) {
        var message = new Message();
        message.token = _config.getBotOauthToken();
        message.channel = event.channel;
        message.text = messages.get(MessageHandler.PLUGIN_INFO);

        return postReply(_config.getPostMessageUrl(), message, _config.getBotOauthToken());
    }
}
