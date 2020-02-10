package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.AuthResponse;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.InteractiveMessage;
import org.catalyst.slackservice.domain.SlackResponse;
import org.catalyst.slackservice.util.SlackLocale;
import org.catalyst.slackservice.util.MessageHandler;

import java.util.concurrent.CompletionStage;

public interface AppService {

    CompletionStage<SlackResponse> postSuggestion(MessageHandler msg, Event event, String correction);

    CompletionStage<AuthResponse> getAuthorization(String requestCode);

    CompletionStage<SlackResponse> postChannelJoin(final MessageHandler msg, final Event event);

    CompletionStage<SlackResponse> postReauthMessage(final MessageHandler msg, final Event event);

    CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage);

    CompletionStage<SlackResponse> postReplacement(InteractiveMessage iMessage, String userToken);

    CompletionStage<SlackResponse> deleteMessage(String responseUrl);

    CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event);

    CompletionStage<SlackLocale> getConversationLocale(String channel);
}
