package services;

import domain.Event;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import play.libs.ws.WSClient;
import play.test.Helpers;
import util.MessageHandler;
import util.MockConfig;


public class SlackServiceTest {
    private WSClient wsClient = Mockito.mock(WSClient.class);
    private MockConfig config = new MockConfig();

    @Test
    public void testGenerateSuggestion() {
        Event event = new Event();

        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";
        SlackService service = new SlackService(config, wsClient);
        MessageHandler msg = new MessageHandler(Helpers.stubMessagesApi().preferred(Mockito.anyCollection()));
        var reply = service.generateSuggestion(msg, event, "she's so thoughtful");

        Assert.assertEquals("message.suggestion", reply.text);
    }

    //TODO!
    @Test
    public void testPostReply(){}
}
