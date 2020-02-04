package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MockSlackService;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.test.WithApplication;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MockConfig;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.NO_CONTENT;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

public class HelpControllerTest extends WithApplication {

    private final static String URI = "/bias-correct/v2/slack/help";
    private final static String COMMAND = "/bias-correct-v2";
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
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
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{"valid_token_123"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        assertEquals(NO_CONTENT, result.status());
    }

    @Test
    public void testUnsupportedAction() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{"valid_token_123"});
        requestBody.put("command", new String[]{COMMAND});
        requestBody.put("text", new String[]{"random"});
        requestBody.put("channel_id", new String[]{"valid_channel_123"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        var body = contentAsBytes(result).toArray();
        var text = Json.parse(body).path("text").textValue();

        assertTrue(text.indexOf("random") > -1);
        assertEquals(OK, result.status());
    }

    @Test
    public void testSpecifyAction() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{"valid_token_123"});
        requestBody.put("command", new String[]{COMMAND});
        requestBody.put("channel_id", new String[]{"valid_channel_123"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);

        var result = route(app, request);
        var body = contentAsBytes(result).toArray();
        var text = Json.parse(body).path("text").textValue();

        assertTrue(text.indexOf("No action specified") > -1);
        assertEquals(OK, result.status());
    }

    @Test
    public void testHelpContent() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{"valid_token_123"});
        requestBody.put("command", new String[]{COMMAND});
        requestBody.put("text", new String[]{"help"});
        requestBody.put("channel_id", new String[]{"valid_channel_123"});
        var request = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=5d8bd31725fd87faa943f947a501e70c4b897e1527481033ccc2d49feb6e74a2")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(OK, result.status());
    }

    @Test
    public void testRequestNotVerified() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{"invalid_token"});
        requestBody.put("command", new String[]{COMMAND});
        requestBody.put("text", new String[]{"help"});
        requestBody.put("channel_id", new String[]{"valid_channel_123"});
        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testEmptyToken() {
        var requestBody = new HashMap<String, String[]>();
        requestBody.put("token", new String[]{});
        requestBody.put("command", new String[]{COMMAND});

        var request = new Http.RequestBuilder()
                .method(POST)
                .uri(URI).bodyFormArrayValues(requestBody);
        var result = route(app, request);
        assertEquals(BAD_REQUEST, result.status());
    }
}
