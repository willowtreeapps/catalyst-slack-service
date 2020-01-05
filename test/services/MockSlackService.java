package services;

import com.fasterxml.jackson.databind.JsonNode;
import util.MessageHandler;

public class MockSlackService implements AppService {

    @Override
    public JsonNode generateSuggestion(MessageHandler msg, String fallback, String channel, String user, String authToken, String correction) {
        return null;
    }

    @Override
    public void postReply(JsonNode reply, String authToken) {

    }
}
