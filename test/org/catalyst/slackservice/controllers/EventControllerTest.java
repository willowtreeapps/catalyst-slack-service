package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.AnalyticsHandler;
import org.catalyst.slackservice.db.MockDbHandler;
import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.db.TokenKey;
import org.catalyst.slackservice.domain.Event;
import org.catalyst.slackservice.domain.SlackResponse;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MessageCorrector;
import org.catalyst.slackservice.services.MockCorrector;
import org.catalyst.slackservice.services.MockSlackService;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.test.WithApplication;

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.test.Helpers.*;

public class EventControllerTest extends WithApplication {
    private static final String EVENTS_URI = "/bias-correct/v2/slack/events";
    private MockDbHandler mockDbHandler = new MockDbHandler();
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(AnalyticsHandler.class).toInstance(mockDbHandler))
                .overrides(bind(TokenHandler.class).toInstance(mockDbHandler))
                .build();
    }

    @Before
    public void setup() {
        TokenKey token = new TokenKey();
        token.userId = "USER123";
        token.teamId = "TEAM123";
        mockDbHandler.setUserToken(token, "xoxp-1234");
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
        eventRequest.type = "event_callback";
        eventRequest.event = new Event();
        eventRequest.event.channel = "valid_channel_123";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Request not verified", error);
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
        var eventRequest = getValidEventCallback();

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
        eventRequest.event.channel = "valid_channel_123";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);

        assertEquals(OK, result.status());
    }

    @Test
    public void testNonBiasedMessage() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.type = "message";
        eventRequest.event.text = "text";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }

    @Test
    public void testBiasCorrected() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.type = "message";
        eventRequest.event.text = "she's so quiet";

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
    public void testBiasedUnauthorizedUser() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.type = "message";
        eventRequest.event.text = "she's so quiet";
        eventRequest.event.user = "USER999";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var slackResponse = Json.fromJson(Json.parse(body), SlackResponse.class);

        assertEquals("23456.78901", slackResponse.messageTs);
        assertEquals(OK, result.status());
    }

    @Test
    public void testRequestVerified() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.type = "message";
        eventRequest.event.text = "she's so quiet";

        var httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=6dedc3a067ddd9c4c93cf4ca6e3ab2cfcc56d89fb85d02c153c81b3b2e8d35f1")
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
        var eventRequest = getValidEventCallback();

        eventRequest.event.type = "message";
        eventRequest.event.text = "unverified message";
        eventRequest.token = null;

        var httpRequest = new Http.RequestBuilder()
                .header("X-Slack-Signature", "v0=2bcd1287bd7880367967d133c9693f5ea9d769b839ea4f6e9572578b8980dda0")
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
        var eventRequest = getValidEventCallback();

        eventRequest.event.text = "invalid";
        eventRequest.event.type = "member_joined_channel";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(BAD_REQUEST, result.status());
    }

    @Test
    public void testChannelJoinSuccessful() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.text = "<@USER123> has joined the channel";
        eventRequest.event.type = "member_joined_channel";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var slackResponse = Json.fromJson(Json.parse(body), SlackResponse.class);

        assertEquals("23456.78901", slackResponse.messageTs);
        assertEquals(OK, result.status());
    }

    @Test
    public void testHelpRequestBotMentioned() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.text = "<@valid_bot_id> help";
        eventRequest.event.type = "message";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }

    @Test
    public void testHelpRequestDirectMessage() {
        var eventRequest = getValidEventCallback();

        eventRequest.event.text = "help";
        eventRequest.event.type = "message";
        eventRequest.event.channelType = "im";

        var httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri(EVENTS_URI).bodyJson(Json.toJson(eventRequest));

        var result = route(app, httpRequest);
        assertEquals(OK, result.status());
    }

    private static EventController.Request getValidEventCallback() {
        var event = new Event();
        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = event;

        event.user = "USER123";
        event.channel = "valid_channel_123";
        event.team = "TEAM123";

        return eventRequest;
    }
}
