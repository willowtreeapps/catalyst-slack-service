package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.Bot;
import org.catalyst.slackservice.db.MockDbHandler;
import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MockSlackService;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.test.WithApplication;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.NO_CONTENT;
import static play.test.Helpers.*;

public class HelpControllerTest extends WithApplication {

    private final static String URI = "/bias-correct/v2/slack/help";
    private final static String COMMAND = "/bias-correct";
    private final MockDbHandler mockDbHandler = new MockDbHandler();

    @Before
    public void setup() {
        Bot bot = new Bot("xoxb-2345", "BOT123", "test");
        mockDbHandler.setBotInfo("TEAM123", bot);
    }

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(TokenHandler.class).toInstance(mockDbHandler))
                .build();
    }

    @Test
    public void testEmptyBody() {
        var requestBody = new HashMap<String, String[]>();
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testNoCommand() {
        var requestBody = getValidHelpRequest();
        requestBody.put("command", new String[]{});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testUnsupportedAction() {
        var requestBody = getValidHelpRequest();
        requestBody.put("text", new String[]{"random"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testSpecifyAction() {
        var requestBody = getValidHelpRequest();
        requestBody.put("text", new String[]{});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testHelpContent() {
        var requestBody = getValidHelpRequest();
        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=cef2363a542926cc08ff21307a25a9a1f08503333664f8259d79ae99aa582a19")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testRequestNotVerified() {
        var requestBody = getValidHelpRequest();
        requestBody.put("token", new String[]{"invalid_token"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testEmptyToken() {
        var requestBody = getValidHelpRequest();
        requestBody.put("token", new String[]{});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    private static HashMap<String, String[]> getValidHelpRequest() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("command", new String[]{COMMAND});
        requestBody.put("token", new String[]{"valid_token_123"});
        requestBody.put("text", new String[]{"help"});
        requestBody.put("channel_id", new String[]{"valid_channel_123"});
        requestBody.put("team_id", new String[]{"TEAM123"});
        requestBody.put("user_id", new String[]{"USER123"});
        return requestBody;
    }
}
