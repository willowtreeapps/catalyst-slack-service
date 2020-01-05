package services;

import io.jsonwebtoken.lang.Assert;
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
        SlackService service = new SlackService(config, wsClient);
        MessageHandler msg = new MessageHandler(Helpers.stubMessagesApi().preferred(Mockito.anyCollection()));
        var reply = service.generateSuggestion(msg, "valid_callback_id", "valid_channel",
                "USER123", "valid_auth_token", "she's so thoughtful");

        Assert.hasText(reply.path("text").textValue());
    }

    //TODO!
    @Test
    public void testPostReply(){}
}
