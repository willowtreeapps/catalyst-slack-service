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
import play.mvc.Result;
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
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI);

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Invalid request", error);
    }

    @Test
    public void testInvalidToken() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "invalid_token";
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Invalid slack token", error);
    }

    @Test
    public void testMissingRequestType() {
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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
        eventRequest.event.text = "";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);

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

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);

        assertEquals(OK, result.status());
    }

    @Test
    public void testNonBiasedMessage() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.text = "text";
        event.user = "USER123";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }

    @Test
    public void testBiasCorrected() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.text = "she's so quiet";
        event.user = "USER123";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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

        event.text = "she's so quiet";
        event.user = "USER123";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=05c3b77a88fd12c192f6010f3eb70dcb58eb66ad96422e79ca4d2763b73d112c")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
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

        event.text = "unverified message";
        event.user = "USER123";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=05c3b77a88fd12c192f6010f3eb70dcb58eb66ad96422e79ca4d2763b73d112c")
                .header("X-Slack-Request-Timestamp", "1578867626")
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Request not verified", error);
    }
}
