package services;

import domain.*;
import util.MessageHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockSlackService implements AppService {
    @Override
    public CompletionStage<SlackResponse> postSuggestion(MessageHandler messages, Event event, String s) {
        var response = new SlackResponse();
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
    public CompletionStage<AuthResponse> getAuthorization(String requestCode) {
        var authResponse = new AuthResponse();

        if (requestCode.equals("invalid_request_1234")) {
            authResponse.ok = false;
            authResponse.error = "request code already used";
        } else {
            authResponse.ok = true;
            authResponse.teamId = "TEAM234";
            authResponse.userId = "USER123";
            authResponse.userToken = "xoxp-token-123";
        }

        return CompletableFuture.completedFuture(authResponse);
    }

    @Override
    public CompletionStage<SlackResponse> postChannelJoinMessage(MessageHandler messages, Event event) {
        var response = new SlackResponse();
        response.ok = true;

        if (event.text.equals("invalid")) {
            response.ok = false;
        }
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public Message generateChannelJoinMessage(MessageHandler msg, Event event) {
        return null;
    }
}
