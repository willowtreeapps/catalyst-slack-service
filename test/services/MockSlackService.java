package services;

import domain.*;
import util.MessageHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockSlackService implements AppService {
    @Override
    public CompletionStage<SlackResponse> postSuggestion(MessageHandler msg, Event event, String correction) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        response.warning = "";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public Message generateSuggestion(MessageHandler msg, Event event, String correction) {
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
    public CompletionStage<SlackResponse> postChannelJoin(MessageHandler msg, Event event) {
        var response = new SlackResponse();
        response.ok = true;

        if (event.text.equals("invalid")) {
            response.ok = false;
        }
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public Message generateUserJoinedMessage(MessageHandler msg, Event event) {
        return null;
    }

    @Override
    public Message generatePluginAddedMessage(MessageHandler msg, Event event) {
        return null;
    }

    @Override
    public CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> postReplacement(MessageHandler msg, InteractiveMessage iMessage, String correction, String userToken) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> deleteMessage(InteractiveMessage iMessage) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }
}
