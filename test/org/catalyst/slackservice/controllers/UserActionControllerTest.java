package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.*;
import org.catalyst.slackservice.domain.Action;
import org.catalyst.slackservice.domain.InteractiveMessage;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.test.WithApplication;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MessageCorrector;
import org.catalyst.slackservice.services.MockCorrector;
import org.catalyst.slackservice.services.MockSlackService;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

public class UserActionControllerTest extends WithApplication {

    private static final String URI = "/bias-correct/v2/slack/actions";
    private MockDbHandler mockDbHandler = new MockDbHandler();
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(TokenHandler.class).toInstance(mockDbHandler))
                .overrides(bind(AnalyticsHandler.class).toInstance(mockDbHandler))
                .build();
    }

    @Before
    public void setup() {
        Bot bot = new Bot();
        bot.token = "xoxb-2345";
        bot.userId = "BOT123";
        mockDbHandler.setBotInfo("TEAM123", bot);
    }

    @Test
    public void testEmptyBody() {
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(null);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testEmptyPayload() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("fake_payload", new String[]{"fake value"});
        requestBody.put("payload", new String[]{""});

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testInvalidPayload() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("payload", new String[] {"invalid body"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testRequestNotVerified() {
        var requestBody = new HashMap<String, String[]>();
        var action = new Action();
        action.name = "she's so quiet";
        action.value = "just_testing";

        var interactiveMessage = getValidInteractiveMessage();
        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));
        interactiveMessage.token = null;

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=invalidhash")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testRequestVerified() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "just_testing";

        var interactiveMessage = getValidInteractiveMessage();
        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=5da66166cdc6af2808a5fc780ea7805f708a3ca8b37a91c89503409fe3f0ba0a")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testMissingIdFields() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "no";

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.responseUrl = "/api/response_url";

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testIgnoreAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "no";

        var interactiveMessage = getValidInteractiveMessage();

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testMissingCallbackIdAndActions() {
        var requestBody = new HashMap<String, String[]>();

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.type = "interactive_message";

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testLearnMoreAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "learn_more";

        var interactiveMessage = getValidInteractiveMessage();

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testBiasCorrectAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "yes";

        var interactiveMessage = getValidInteractiveMessage();

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testBiasCorrectEmptyCorrectionAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's great";
        action.value = "yes";

        var interactiveMessage = getValidInteractiveMessage();
        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testUnsupportedAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "unknown_name";
        action.value = "unsupported_action";

        var interactiveMessage = getValidInteractiveMessage();
        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    private static InteractiveMessage getValidInteractiveMessage() {
        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.team.id = "TEAM123";
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.channel.id = "CHANNEL123";
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.user.id = "USER123";
        interactiveMessage.user.name = "Test User";
        interactiveMessage.responseUrl = "/api/response_url";

        return interactiveMessage;
    }
}
