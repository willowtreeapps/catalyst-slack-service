package services;

import com.fasterxml.jackson.databind.JsonNode;
import util.MessageHandler;

public interface AppService {
    JsonNode generateSuggestion(MessageHandler msg, String fallback, String channel, String user, String authToken, String correction);
    void postReply(JsonNode reply, String authToken);
}
