package services;

import domain.Event;
import domain.SlackResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import util.MessageHandler;
import util.MockConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static play.mvc.Results.ok;

public class SlackServiceTest {
    private WSClient wsClient;
    private Server server;
    private SlackService service;
    private MessageHandler msg;
    private MockConfig config;

    @Before
    public void setup() {
        config = new MockConfig();

        var messagesMap = Collections.singletonMap("message.suggestion", "Suggested correction");
        var langs = new Langs(new play.api.i18n.DefaultLangs());
        var langMap = Collections.singletonMap(Lang.defaultLang().code(), messagesMap);
        var messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);
        var messages = messagesApi.preferred(langs.availables());
        msg = new MessageHandler(messages);

        var ec = new HttpExecutionContext(ForkJoinPool.commonPool());

        var response = new SlackResponse();
        response.ok = true;
        response.messageTs = "12345.67890";
        response.warning = "";

        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)
                .POST(config.getPostUrl())
                .routingTo( request ->
                        ok(Json.toJson(response))
                )
                .build());

        wsClient = play.test.WSTestClient.newClient(server.httpPort());
        service = new SlackService(ec, config, wsClient);
    }

    @After
    public void tearDown() throws IOException {
        try {
            wsClient.close();
        } finally {
            server.stop();
        }
    }

    @Test
    public void testGenerateSuggestion() {
        Event event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var correction = "she's so thoughtful";

        var reply = service.generateSuggestion(msg, event, config.getAppOauthToken(), correction);

        Assert.assertEquals("Suggested correction", reply.text);
        Assert.assertEquals(correction, reply.attachments.get(0).actions.get(0).name);
        Assert.assertEquals(event.user, reply.user);
    }

    @Test
    public void testPostReply() throws Exception {
        Event event = new Event();
        var response = service.postSuggestion(msg, event, "new correction")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals("12345.67890", response.messageTs);
    }
}
