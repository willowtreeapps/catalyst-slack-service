package controllers;

import db.DbManager;
import db.MockDbManager;
import domain.Event;
import domain.SlackResponse;
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

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.test.Helpers.*;

public class EventControllerTest extends WithApplication {
    private static final String EVENTS_URI = "/bias-correct/v2/slack/events";

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(DbManager.class).to(MockDbManager.class))
                .build();
    }

    @Test
    public void testInvalidRequest() {
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI);

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Invalid request", error);
    }

    @Test
    public void testInvalidToken() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "invalid_token";
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Invalid slack token", error);
    }

    @Test
    public void testMissingRequestType() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Missing request type", error);
    }

    @Test
    public void testUnsupportedRequest() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "unsupported_request_type";
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Unsupported request type", error);
    }

    @Test
    public void testMissingUrlVerificationChallenge() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "url_verification";
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Missing challenge parameter", error);
    }

    @Test
    public void testUrlVerification() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "url_verification";
        eventRequest.challenge = "valid_challenge_123";
        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var challenge = Json.parse(body).path("challenge").textValue();

        assertEquals(OK, result.status());
        assertEquals(eventRequest.challenge, challenge);
    }

    @Test
    public void testEmptyEvent() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Invalid event body", error);
    }

    @Test
    public void testEmptyMessage() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = new Event();
        eventRequest.event.type = "message";
        eventRequest.event.text = "";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);

        assertEquals(OK, result.status());
    }

    @Test
    public void testBotMessageIgnored() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = new Event();
        eventRequest.event.text = "bot message";
        eventRequest.event.botId = "valid_bot_id";
        eventRequest.event.username = "valid_bot_username";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);

        assertEquals(OK, result.status());
    }

    @Test
    public void testNonBiasedMessage() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.type = "message";
        event.text = "text";
        event.user = "USER123";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }

    @Test
    public void testBiasCorrected() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.type = "message";
        event.text = "she's so quiet";
        event.user = "USER123";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var slackResponse = Json.fromJson(Json.parse(body), SlackResponse.class);

        assertEquals("12345.67890", slackResponse.messageTs);
        assertEquals(OK, result.status());
    }

    @Test
    public void testSigningSecret() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.type = "message";
        event.text = "she's so quiet";
        event.user = "USER123";

        var httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=53322ed06395d118dd3e25e58eae762e50f6478f27d03135bb4bebf501173c06")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var slackResponse = Json.fromJson(Json.parse(body), SlackResponse.class);

        assertEquals("12345.67890", slackResponse.messageTs);
        assertEquals(OK, result.status());
    }

    @Test
    public void testRequestNotVerified() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.type = "message";
        event.text = "unverified message";
        event.user = "USER123";

        var httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=53322ed06395d118dd3e25e58eae762e50f6478f27d03135bb4bebf501173c06")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Request not verified", error);
    }

    @Test
    public void testChannelJoinFailed() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.text = "invalid";
        event.user = "USER123";
        event.type = "message";
        event.subtype = "channel_join";
        event.channel = "valid_channel";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testChannelJoinSuccessful() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.text = "<@USER123> has joined the channel";
        event.user = "USER123";
        event.type = "message";
        event.subtype = "channel_join";
        event.channel = "valid_channel";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }
}
