package services;

import domain.Event;
import domain.Message;
import util.MessageHandler;

public class MockSlackService implements AppService {

    @Override
    public Message generateSuggestion(MessageHandler msg, Event event, String correction) {
        return null;
    }

    @Override
    public void postReply(Message reply, String authToken) {

    }
}
