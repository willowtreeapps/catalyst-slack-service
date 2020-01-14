package services;

import domain.AuthResponse;
import domain.Event;
import domain.Message;
import domain.SlackResponse;
import util.MessageHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockSlackService implements AppService {
    @Override
    public CompletionStage<SlackResponse> postSuggestion(MessageHandler messages, Event event, String s) {
        SlackResponse response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        response.warning = "";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public Message generateSuggestion(MessageHandler msg, Event event, String authToken, String correction) {
        return null;
    }

    @Override
    public CompletionStage<AuthResponse> getAuth(String requestCode) {
        return null;
    }
}
