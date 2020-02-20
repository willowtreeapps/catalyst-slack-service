package org.catalyst.slackservice.services;

import org.catalyst.slackservice.db.Bot;
import org.catalyst.slackservice.domain.*;
import org.catalyst.slackservice.util.SlackLocale;
import org.catalyst.slackservice.util.MessageHandler;

import java.util.concurrent.CompletionStage;

public interface AppService {

    CompletionStage<SlackResponse> postSuggestion(MessageHandler msg, Event event, String correction, Bot bot);

    CompletionStage<AuthResponse> getAuthorization(String requestCode);

    CompletionStage<SlackResponse> postChannelJoin(MessageHandler messages, Event event, Bot bot);

    CompletionStage<SlackResponse> postReauthMessage(MessageHandler messages, Event event, Bot bot);

    CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage, Bot bot);

    CompletionStage<SlackResponse> postReplacement(InteractiveMessage iMessage, String userToken);

    CompletionStage<SlackResponse> deleteMessage(String responseUrl);

    CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event, Bot bot);

    CompletionStage<SlackLocale> getConversationLocale(String channel, Bot bot);

    CompletionStage<SlackResponse> postCustomMessage(String url, Message message, Bot bot);
}
