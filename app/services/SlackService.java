package services;

import domain.*;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.LinkedList;
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

    public Message generateSuggestion(MessageHandler msg, Event event, String authToken, String correction) {
        var actions = new LinkedList<Action>();
        actions.add(new Action(correction, msg.get("button.correct"), "yes", "primary"));
        actions.add(new Action(correction, msg.get("button.no"), "no", "danger"));
        actions.add(new Action(correction, msg.get("button.learn"), "learn_more", null));

        var attachments = new LinkedList<Attachment>();
        attachments.add(new Attachment(msg.get("message.fallback"), msg.get("message.title"), event.ts, actions));

        var message = new Message(event.channel, authToken, event.user, msg.get("message.suggestion",correction), attachments);

        return message;
    }

    public CompletionStage<SlackResponse> postSuggestion(final MessageHandler messages, final Event event, final String correction) {
        var botReply = generateSuggestion(messages, event, _config.getAppOauthToken(), correction);
        //TODO: check for team tokens as auth parameter? see original code for reference
        return postReply(botReply, _config.getAppOauthToken());
    }

    private CompletionStage<SlackResponse> postReply(Message reply, String authToken) {

        var request = _wsClient.url(_config.getPostUrl()).
                setContentType("application/json").
                addHeader("Authorization", String.format("Bearer %s", authToken));

        var jsonPromise = request.post(Json.toJson(reply));

        return jsonPromise.thenApplyAsync(r ->
            Json.fromJson(r.getBody(json()), SlackResponse.class)
        , _ec.current());
    }

    public CompletionStage<AuthResponse> getAuth(String requestCode) {
        var request = _wsClient.url(_config.getOauthUrl()).
                addQueryParameter("code", requestCode).
                addQueryParameter("client_id", _config.getClientId()).
                addQueryParameter("client_secret", _config.getClientSecret());

        var jsonPromise = request.get();

        return jsonPromise.thenApplyAsync(r ->
            Json.fromJson(r.getBody(json()), AuthResponse.class)
        , _ec.current());
    }
}
