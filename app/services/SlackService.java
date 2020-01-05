package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SlackService implements AppService, WSBodyReadables {

    private static class Message {
        public String ok = "true";
        public String channel;
        public String token;
        public String user;
        public String as_user = "false";
        public String text;
        public List<Attachment> attachments;

        Message(String channel, String token, String user, String text, List<Attachment> attachments) {
            this.channel = channel;
            this.token = token;
            this.user = user;
            this.text = text;
            this.attachments = attachments;
        }
    }

    private static class Attachment {
        public String fallback;
        public String title;
        public String callback_id;
        public String attachment_type = "default";
        public List<Action> actions;

        Attachment(String fallback, String title, String callbackId, List<Action> actions) {
            this.fallback = fallback;
            this.title = title;
            this.callback_id = callbackId;
            this.actions = actions;
        }
    }

    private static class Action {
        public String name;
        public String text;
        public String type = "button";
        public String value;
        public String style; // optional?

        Action(String name, String text, String value, String style) {
            this.name = name;
            this.text = text;
            this.value = value;
            this.style = style;
        }
    }

    private final WSClient _wsClient;
    private final AppConfig _config;

    @Inject
    SlackService(AppConfig config, WSClient wsClient) {
        this._wsClient = wsClient;
        this._config = config;
    }

    @Override
    public JsonNode generateSuggestion(
            MessageHandler msg, String callbackId, String channel,
            String user, String authToken, String correction) {

        var actions = new LinkedList<Action>();
        actions.add(new Action(correction, msg.get("button.correct"), "yes", "primary"));
        actions.add(new Action(correction, msg.get("button.no"), "no", "danger"));
        actions.add(new Action(correction, msg.get("button.learn"), "learn_more", null));

        var attachments = new LinkedList<Attachment>();
        attachments.add(new Attachment(msg.get("message.fallback"), msg.get("message.title"), callbackId, actions));

        var message = new Message(channel, authToken, user, msg.get("message.suggestion",correction), attachments);

        return Json.toJson(message);
    }

    @Override
    public void postReply(JsonNode reply, String authToken){
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
