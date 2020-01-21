package controllers;

import db.AnalyticsHandler;
import db.MockDbHandler;
import db.TokenHandler;
import domain.Action;
import domain.InteractiveMessage;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.test.WithApplication;
import services.AppService;
import services.MessageCorrector;
import services.MockCorrector;
import services.MockSlackService;
import util.AppConfig;
import util.MockConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

public class UserActionControllerTest extends WithApplication {

    private MockDbHandler dbManager = new MockDbHandler();
    private static final String URI = "/bias-correct/v2/slack/actions";
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(TokenHandler.class).to(MockDbHandler.class))
                .overrides(bind(AnalyticsHandler.class).to(MockDbHandler.class))
                .build();
    }

    @Test
    public void testEmptyBody() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("payload", new String[]{});

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testInvalidPayload() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("payload", new String[] {"invalid body", "invalid body"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testRequestNotVerified() {
        var requestBody = new HashMap<String, String[]>();
        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.type = "interactive_message";
        interactiveMessage.actions = new ArrayList<>();

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=53322ed06395d118dd3e25e58eae762e50f6478f27d03135bb4bebf501173c06")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testRequestVerified() {
        var requestBody = new HashMap<String, String[]>();
        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.type = "interactive_message";
        interactiveMessage.actions = new ArrayList<>();

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=ca68c4a0cd5e115d1b4825f87af202e67ee8fbada73d4b3a1cc70dd45557f6af")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testNoAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "no";

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.triggerId = "910111213145.123456789101.bf6855a370012f701dacec1ea733eb82";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.team.id = "TEAM123";
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.channel.id = "CHANNEL123";
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.user.id = "USER123";
        interactiveMessage.user.name = "Test User";
        interactiveMessage.responseUrl = "/api/response_url";

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(OK, result.status());
    }

    @Test
    public void testLearnMoreAction() {
        var requestBody = new HashMap<String, String[]>();

        var action = new Action();
        action.name = "she's so quiet";
        action.value = "learn_more";

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.triggerId = "910111213145.123456789101.bf6855a370012f701dacec1ea733eb82";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.team.id = "TEAM123";
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.channel.id = "CHANNEL123";
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.user.id = "USER123";
        interactiveMessage.user.name = "Test User";

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

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.triggerId = "910111213145.123456789101.bf6855a370012f701dacec1ea733eb82";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.team.id = "TEAM123";
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.channel.id = "CHANNEL123";
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.user.id = "USER123";
        interactiveMessage.user.name = "Test User";

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

        var interactiveMessage = new InteractiveMessage();
        interactiveMessage.token = "valid_token_123";
        interactiveMessage.type = "interactive_message";
        interactiveMessage.callbackId = "1234567890.000000";
        interactiveMessage.triggerId = "910111213145.123456789101.bf6855a370012f701dacec1ea733eb82";
        interactiveMessage.team = new InteractiveMessage.Team();
        interactiveMessage.team.id = "TEAM123";
        interactiveMessage.channel = new InteractiveMessage.Channel();
        interactiveMessage.channel.id = "CHANNEL123";
        interactiveMessage.user = new InteractiveMessage.User();
        interactiveMessage.user.id = "USER123";
        interactiveMessage.user.name = "Test User";

        interactiveMessage.actions = new ArrayList<>(Arrays.asList(action));

        var payload = new String[]{Json.toJson(interactiveMessage).toString()};
        requestBody.put("payload", payload);

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

}
