package org.catalyst.slackservice.services;

import org.catalyst.slackservice.db.Bot;
import org.catalyst.slackservice.domain.*;
import org.catalyst.slackservice.util.MessageHandler;
import org.catalyst.slackservice.util.SlackLocale;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockSlackService implements AppService {
    @Override
    public CompletionStage<SlackResponse> postSuggestion(MessageHandler msg, Event event, String correction, Bot bot) {
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
            authResponse.team.name = "team-234";
            authResponse.user = new AuthResponse.AuthedUser();
            authResponse.user.id = "USER123";
            authResponse.user.accessToken = "xoxp-token-123";
            authResponse.tokenType = "bot";
            authResponse.botUserId = "BOT123";
            authResponse.accessToken = "xoxb-token-234";
        }

        return CompletableFuture.completedFuture(authResponse);
    }

    @Override
    public CompletionStage<SlackResponse> postChannelJoin(MessageHandler msg, Event event, Bot bot) {
        var response = new SlackResponse();
        response.ok = true;

        if (event.text.equals("invalid")) {
            response.ok = false;
        }
        response.messageTs = "23456.78901";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> postReauthMessage(MessageHandler msg, Event event, Bot bot) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "34567.89012";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage, Bot bot) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackResponse> postReplacement(InteractiveMessage iMessage, String userToken) {
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
    public CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event, Bot bot) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletionStage<SlackLocale> getConversationLocale(String channel, Bot bot) {
        return CompletableFuture.completedFuture(new SlackLocale());
    }

    @Override
    public CompletionStage<SlackResponse> postCustomMessage(String url, Message message, Bot bot) {
        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "45678.90123";
        return CompletableFuture.completedFuture(response);
    }
}
