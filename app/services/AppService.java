package services;

import domain.Event;
import domain.Message;
import domain.SlackResponse;
import util.MessageHandler;

import java.util.concurrent.CompletionStage;

public interface AppService {

    CompletionStage<SlackResponse> postSuggestion(MessageHandler messages, Event event, String s);

    Message generateSuggestion(MessageHandler msg, Event event, String authToken, String correction);
}
