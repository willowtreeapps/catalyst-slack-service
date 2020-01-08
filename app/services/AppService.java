package services;

import domain.Event;
import domain.Message;
import util.MessageHandler;

public interface AppService {
    Message generateSuggestion(MessageHandler msg, Event event, String correction);
    void postReply(Message reply, String authToken);
}
