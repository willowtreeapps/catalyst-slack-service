package services;

import domain.Event;
import domain.Message;
import domain.SlackResponse;
import util.MessageHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockSlackService implements AppService {
    @Override
    public CompletionStage<SlackResponse> postSuggestion(MessageHandler messages, Event event, String s) {
        return CompletableFuture.completedFuture(new SlackResponse());
    }

    @Override
    public Message generateSuggestion(MessageHandler msg, Event event, String authToken, String correction) {
        return null;
    }
}
