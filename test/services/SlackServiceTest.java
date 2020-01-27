package services;

import domain.*;
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
import util.MessageGenerator;
import util.MessageHandler;
import util.MockConfig;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static play.mvc.Results.ok;

public class SlackServiceTest {
    private WSClient wsClient;
    private Server server;
    private SlackService service;
    private MessageHandler msg;
    private MockConfig config;

    private final static String INTERACTIVE_RESPONSE_URL = "/response_url/1234";
    private final static String EPHEMERAL_TS = "12345.67890";
    private final static String MESSAGE_TS = "23456.78901";
    private final static String UPDATE_TS = "34567.89012";
    @Before
    public void setup() {
        config = new MockConfig();

        Map<String, String> messagesMap = new HashMap<>();
        messagesMap.put("message.suggestion", "Suggested correction");
        messagesMap.put("message.gender.bias.info", "Gender bias information");
        messagesMap.put("message.learn.more", "Plugin information");

        var langs = new Langs(new play.api.i18n.DefaultLangs());
        var langMap = Collections.singletonMap(Lang.defaultLang().code(), messagesMap);
        var messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);
        var messages = messagesApi.preferred(langs.availables());
        msg = new MessageHandler(messages);

        var ec = new HttpExecutionContext(ForkJoinPool.commonPool());

        var ephResponse = new SlackResponse();
        ephResponse.ok = true;
        ephResponse.messageTs = EPHEMERAL_TS;
        ephResponse.warning = "";

        var msgResponse = new SlackResponse();
        msgResponse.ok = true;
        msgResponse.messageTs = MESSAGE_TS;
        msgResponse.warning = "";

        var updResponse = new SlackResponse();
        updResponse.ok = true;
        updResponse.messageTs = UPDATE_TS;
        updResponse.warning = "";

        var authResponse = new AuthResponse();
        authResponse.ok = true;
        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)

                .POST(config.getPostEphemeralUrl())
                .routingTo( request -> ok(Json.toJson(ephResponse)))

                .POST(config.getPostMessageUrl())
                .routingTo(request -> ok(Json.toJson(msgResponse)))

                .GET(config.getOauthUrl())
                .routingTo(request -> ok(Json.toJson(authResponse)))

                .POST(config.getUpdateUrl())
                .routingTo(request -> ok(Json.toJson(updResponse)))

                .POST(INTERACTIVE_RESPONSE_URL)
                .routingTo(request -> ok(Json.toJson(ephResponse)))

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
        event.text = "she's so quiet";

        var correction = "she's so thoughtful";

        var reply = MessageGenerator.generateSuggestion(msg, event, correction, config.getBotOauthToken());

        Assert.assertEquals("Suggested correction", reply.text);
        Assert.assertEquals(event.text, reply.attachments.get(0).actions.get(0).name);
        Assert.assertEquals(event.user, reply.user);
    }

    @Test
    public void testPostSuggestion() throws Exception {
        var event = new Event();
        var response = service.postSuggestion(msg, event, "new correction")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(EPHEMERAL_TS, response.messageTs);
    }

    @Test
    public void testGeneratePluginAddedMessage() {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = config.getBotId();

        var reply = MessageGenerator.generatePluginAddedMessage(msg, event,
                config.getBotOauthToken(), config.getAppSigninUrl(), config.getLearnMoreUrl());

        Assert.assertEquals("Gender bias information", reply.text);
        Assert.assertEquals(null, reply.user);
    }

    @Test
    public void testGenerateUserJoinedMessage() {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var reply = MessageGenerator.generateUserJoinedMessage(msg, event,
                config.getBotOauthToken(), config.getAppSigninUrl(), config.getLearnMoreUrl());

        Assert.assertEquals("Gender bias information", reply.text);
        Assert.assertEquals(event.user, reply.user);
    }

    @Test
    public void testPostUserJoin() throws Exception {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var response = service.postChannelJoin(msg, event)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(EPHEMERAL_TS, response.messageTs);
    }

    @Test
    public void testPostPluginAdded() throws Exception {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "valid_bot_id";

        var response = service.postChannelJoin(msg, event)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(MESSAGE_TS, response.messageTs);
    }

    @Test
    public void testGetAuthorization() throws Exception {
        var event = new Event();
        event.ts = "valid_callback_id";
        event.channel = "valid_channel";
        event.user = "USER123";

        var response = service.getAuthorization("code_1234")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
    }

    @Test
    public void testLearnMore() throws Exception {
        var iMessage = new InteractiveMessage();
        iMessage.triggerId = "valid_trigger_id";
        iMessage.channel = new InteractiveMessage.Channel();
        iMessage.channel.id = "valid_channel";
        iMessage.user = new InteractiveMessage.User();
        iMessage.user.id = "USER123";

        var response = service.postLearnMore(msg, iMessage)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
    }

    @Test
    public void testPostReplacementNullToken() throws Exception {
        var iMessage = new InteractiveMessage();
        iMessage.triggerId = "valid_trigger_id";
        iMessage.channel = new InteractiveMessage.Channel();
        iMessage.channel.id = "valid_channel";
        iMessage.user = new InteractiveMessage.User();
        iMessage.user.id = "USER123";
        iMessage.callbackId = "valid_callback_id";

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "yes";
        iMessage.actions = new ArrayList<>(Arrays.asList(action));

        var response = service.postReplacement(msg, iMessage, "she's so thoughtful", null)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(MESSAGE_TS, response.messageTs);
    }

    @Test
    public void testPostReplacementExistingToken() throws Exception {
        var iMessage = new InteractiveMessage();
        iMessage.triggerId = "valid_trigger_id";
        iMessage.channel = new InteractiveMessage.Channel();
        iMessage.channel.id = "valid_channel";
        iMessage.user = new InteractiveMessage.User();
        iMessage.user.id = "USER123";
        iMessage.callbackId = "valid_callback_id";

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "yes";
        iMessage.actions = new ArrayList<>(Arrays.asList(action));

        var response = service.postReplacement(msg, iMessage, "she's so thoughtful", "xoxp-token-123")
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(UPDATE_TS, response.messageTs);
    }

    @Test
    public void testDeleteMessage() throws Exception {
        var iMessage = new InteractiveMessage();
        iMessage.responseUrl = INTERACTIVE_RESPONSE_URL;

        var response = service.deleteMessage(iMessage)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(EPHEMERAL_TS, response.messageTs);
    }

    @Test
    public void testPostHelpMessage() throws Exception {
        var event = new Event();
        event.channel = "valid_channel";
        event.user = "USER123";

        var response = service.postHelpMessage(msg, event)
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        Assert.assertEquals(true, response.ok);
        Assert.assertEquals(MESSAGE_TS, response.messageTs);
    }

}
