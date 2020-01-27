package services;

import domain.AuthResponse;
import domain.Event;
import domain.InteractiveMessage;
import domain.SlackResponse;
import util.MessageHandler;

import java.util.concurrent.CompletionStage;

public interface AppService {

    CompletionStage<SlackResponse> postSuggestion(MessageHandler msg, Event event, String correction);

    CompletionStage<AuthResponse> getAuthorization(String requestCode);

    CompletionStage<SlackResponse> postChannelJoin(final MessageHandler msg, final Event event);

    CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage);

    CompletionStage<SlackResponse> postReplacement(MessageHandler msg, InteractiveMessage iMessage, String correction, String userToken);

    CompletionStage<SlackResponse> deleteMessage(InteractiveMessage iMessage);

    CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event);
}
