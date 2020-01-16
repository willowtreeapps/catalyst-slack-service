package services;

import domain.AuthResponse;
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
import java.util.HashMap;
import java.util.Map;
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

        Map<String, String> messagesMap = new HashMap<>();
        messagesMap.put("message.suggestion", "Suggested correction");
        messagesMap.put("message.gender.bias.info", "Gender bias information");

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

        var authResponse = new AuthResponse();
        authResponse.ok = true;
        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)
                .POST(config.getPostEphemeralUrl())
                .routingTo( request -> ok(Json.toJson(response))
                ).POST(config.getPostMessageUrl())
                .routingTo(request -> ok(Json.toJson(response)))
                .GET(config.getOauthUrl())
                .routingTo(request -> ok(Json.toJson(authResponse)))
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
        var event = new Event();
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
    public void testPostSuggestion() throws Exception {
        var event = new Event();
        var response = service.postSuggestion(msg, event, "new correction")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals("12345.67890", response.messageTs);
    }

    @Test
    public void testGeneratePluginAddedMessage() {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = config.getBotId();

        var reply = service.generateChannelJoinMessage(msg, event);

        Assert.assertEquals("Gender bias information", reply.text);
        Assert.assertEquals(null, reply.user);
    }

    @Test
    public void testGenerateUserJoinedMessage() {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var reply = service.generateChannelJoinMessage(msg, event);

        Assert.assertEquals("Gender bias information", reply.text);
        Assert.assertEquals(event.user, reply.user);
    }

    @Test
    public void testPostChannelJoinMessage() throws Exception {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var response = service.postChannelJoinMessage(msg, event)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals("12345.67890", response.messageTs);
    }

    @Test
    public void testGetAuth() throws Exception {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var response = service.getAuthorization("code_1234")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
    }
}
