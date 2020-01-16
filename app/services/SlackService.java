package services;

import domain.*;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class SlackService implements AppService, WSBodyReadables {

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
        var actions = new ArrayList<Action>();
        actions.add(new Action(event.text, msg.get("button.correct"), "yes", "primary", null));
        actions.add(new Action(event.text, msg.get("button.no"), "no", "danger", null));
        actions.add(new Action(event.text, msg.get("button.learn"), "learn_more", null, null));

        var attachments = new ArrayList<Attachment>();
        attachments.add(new Attachment(msg.get("message.fallback"), msg.get("message.title"), event.ts, actions));

        var message = new Message(event.channel, _config.getAppOauthToken(), event.user, msg.get("message.suggestion",correction), attachments);

        return message;
    }

    public CompletionStage<SlackResponse> postSuggestion(final MessageHandler messages, final Event event, final String correction) {
        var botReply = generateSuggestion(messages, event, correction);
        return postReply(_config.getPostEphemeralUrl(), botReply, _config.getAppOauthToken());
    }

    private CompletionStage<SlackResponse> postReply(String url, Message reply, String authToken) {

        var request = _wsClient.url(url).
                setContentType("application/json").
                addHeader("Authorization", String.format("Bearer %s", authToken));

        var jsonPromise = request.post(Json.toJson(reply));

        return jsonPromise.thenApplyAsync(r ->
            Json.fromJson(r.getBody(json()), SlackResponse.class)
        , _ec.current());
    }

    private Message generateChannelJoinMessage(MessageHandler msg, Event event) {
        var actions = new ArrayList<Action>();
        actions.add(new Action(null, msg.get("button.authorize"), "yes", "primary", _config.getAppSigninUrl()));
        actions.add(new Action(null, msg.get("button.learn"), "learn_more", null, _config.getLearnMoreUrl()));

        var attachments = new ArrayList<Attachment>();
        attachments.add(new Attachment(msg.get("message.fallback"), null, null, actions));

        var message = new Message(event.channel, _config.getAppOauthToken(), null, null, attachments);

        return message;
    }

    public Message generatePluginAddedMessage(MessageHandler msg, Event event) {
        var message = generateChannelJoinMessage(msg, event);
        var leadText = msg.get("message.plugin.added");
        var fullText = msg.get("message.gender.bias.info", leadText);
        message.text = fullText;

        return message;
    }

    public Message generateUserJoinedMessage(MessageHandler msg, Event event) {
        var message = generateChannelJoinMessage(msg, event);
        var leadText = msg.get("message.user.joined");
        var fullText = msg.get("message.gender.bias.info", leadText);
        message.text = fullText;
        message.user = event.user;

        return message;
    }

    public CompletionStage<SlackResponse> postChannelJoin(final MessageHandler messages, final Event event) {

        String url;
        Message message;
        if (event.user == null || event.user.equals(_config.getBotId())) {
            url = _config.getPostMessageUrl();
            message = generatePluginAddedMessage(messages, event);
        } else {
            url = _config.getPostEphemeralUrl();
            message = generateUserJoinedMessage(messages, event);
        }

        return postReply(url, message, _config.getAppOauthToken());
    }

    public CompletionStage<AuthResponse> getAuthorization(String requestCode) {
        var request = _wsClient.url(_config.getOauthUrl()).
                addQueryParameter("code", requestCode).
                addQueryParameter("client_id", _config.getClientId()).
                addQueryParameter("client_secret", _config.getClientSecret());

        var jsonPromise = request.get();

        return jsonPromise.thenApplyAsync(r ->
            Json.fromJson(r.getBody(json()), AuthResponse.class)
        , _ec.current());
    }

    public CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage) {
        var message = new Message( iMessage.channel.id, _config.getAppOauthToken(),
                iMessage.user.id, msg.get("message.learn.more"), null);
        message.triggerId = iMessage.triggerId;
        message.asUser = true;

        return postReply(_config.getPostEphemeralUrl(), message, _config.getAppOauthToken());
    }

    public CompletionStage<SlackResponse> postReplacement(
            MessageHandler msg, InteractiveMessage iMessage, String correction, String userToken) {

        var originalPost = iMessage.actions.get(0).name;
        var message = new Message( iMessage.channel.id, userToken, iMessage.user.id, correction, null);
        message.triggerId = iMessage.triggerId;
        message.ts = iMessage.callbackId;

        var url = _config.getUpdateUrl();

        if (userToken == null) {
            message.token = _config.getAppOauthToken();
            message.text = msg.get("message.replaced.with", iMessage.user.name, originalPost, correction);

            url = _config.getPostMessageUrl();
        }

        return postReply(url, message, message.token);
    }

    public CompletionStage<SlackResponse> deleteMessage(InteractiveMessage iMessage) {

        var request = _wsClient.url(iMessage.responseUrl).
                setContentType("application/json");

        var jsonPromise = request.post(Json.toJson(Map.of("delete_original", Boolean.valueOf(true))));

        return jsonPromise.thenApplyAsync(r ->
                        Json.fromJson(r.getBody(json()), SlackResponse.class)
                , _ec.current());
    }
}
