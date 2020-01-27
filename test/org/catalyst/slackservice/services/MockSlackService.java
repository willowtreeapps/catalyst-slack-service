package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.AuthResponse;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.InteractiveMessage;
import org.catalyst.slackservice.domain.SlackResponse;
import org.catalyst.slackservice.util.MessageHandler;

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
    public CompletionStage<AuthResponse> getAuthorization(String requestCode) {
        var authResponse = new AuthResponse();

        if (requestCode.equals("invalid_request_1234")) {
            authResponse.ok = false;
            authResponse.error = "request code already used";
        } else {
            authResponse.ok = true;
            authResponse.team = new AuthResponse.Team();
            authResponse.team.id = "TEAM234";
            authResponse.user = new AuthResponse.AuthedUser();
            authResponse.user.id = "USER123";
            authResponse.user.accessToken = "xoxp-token-123";
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
    public CompletionStage<SlackResponse> deleteMessage(String responseUrl) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }
}
