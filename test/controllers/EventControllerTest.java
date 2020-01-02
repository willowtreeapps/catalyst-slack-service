package controllers;

import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import util.AppConfig;
import util.MockConfig;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.*;
import static play.inject.Bindings.bind;

public class EventControllerTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().overrides(bind(AppConfig.class).to(MockConfig.class)).build();
    }

    @Test
    public void testInvalidRequest() {
        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri("/bias-correct/v2/slack/events");

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
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

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
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

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
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

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
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

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
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var challenge = Json.parse(body).path("challenge").textValue();

        assertEquals(OK, result.status());
        assertEquals(eventRequest.challenge, challenge);
    }
}
