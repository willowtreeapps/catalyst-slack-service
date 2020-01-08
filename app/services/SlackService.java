package services;

import domain.Action;
import domain.Attachment;
import domain.Event;
import domain.Message;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.LinkedList;

public class SlackService implements AppService, WSBodyReadables {

    private final WSClient _wsClient;
    private final AppConfig _config;

    @Inject
    SlackService(AppConfig config, WSClient wsClient) {
        this._wsClient = wsClient;
        this._config = config;
    }

    @Override
    public Message generateSuggestion(
            MessageHandler msg, Event event, String correction) {

        var actions = new LinkedList<Action>();
        actions.add(new Action(correction, msg.get("button.correct"), "yes", "primary"));
        actions.add(new Action(correction, msg.get("button.no"), "no", "danger"));
        actions.add(new Action(correction, msg.get("button.learn"), "learn_more", null));

        var attachments = new LinkedList<Attachment>();
        attachments.add(new Attachment(msg.get("message.fallback"), msg.get("message.title"), event.ts, actions));

        var message = new Message(event.channel, _config.getAppOauthToken(), event.user, msg.get("message.suggestion",correction), attachments);

        return message;
    }

    @Override
    public void postReply(Message reply, String authToken){

        var request = _wsClient.url(_config.getPostUrl()).
                setContentType("application/json").
                addHeader("Authorization", String.format("Bearer %s", authToken));

        var jsonPromise = request.post(reply).thenApply(r -> r.getBody(json()));

        JsonNode responseNode = null;
        try {
            responseNode = jsonPromise.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Logger.of("SlackService").info(responseNode.toString());
    }
}
