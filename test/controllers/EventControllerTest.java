package controllers;

import org.junit.Test;
import org.mockito.Mockito;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.BiasCorrectService;
import util.AppConfig;
import util.MockConfig;

import static org.junit.Assert.assertEquals;
import static play.inject.Bindings.bind;
import static play.test.Helpers.*;

public class EventControllerTest extends WithApplication {
    private static final BiasCorrectService mockService = Mockito.mock(BiasCorrectService.class);

    @Override
    protected Application provideApplication() {

        return new GuiceApplicationBuilder().overrides(bind(AppConfig.class).to(MockConfig.class))
                .bindings(bind(BiasCorrectService.class).toInstance(mockService))
                .build();
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

    @Test
    public void testBiasCorrectNoChange() {
        BiasCorrectService.CorrectResponse response = new BiasCorrectService.CorrectResponse();
        response.correction = "";
        Mockito.when(mockService.correct(Mockito.any())).thenReturn(response);

        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = new EventController.Event();
        eventRequest.event.text = "text";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var challenge = Json.parse(body);

        assertEquals(OK, result.status());
    }

    @Test
    public void testBiasCorrected() {
        BiasCorrectService.CorrectResponse response = new BiasCorrectService.CorrectResponse();
        response.correction = "she's so thoughtful";
        Mockito.when(mockService.correct(Mockito.any())).thenReturn(response);

        var eventRequest = new EventController.Request();
        eventRequest.token = "valid_token_123";
        eventRequest.type = "event_callback";
        eventRequest.event = new EventController.Event();
        eventRequest.event.text = "she's so quiet";

        Http.RequestBuilder httpRequest = new Http.RequestBuilder()
                .method(POST)
                .uri("/bias-correct/v2/slack/events").bodyJson(Json.toJson(eventRequest));

        Result result = route(app, httpRequest);
        var body = contentAsBytes(result).toArray();
        var challenge = Json.parse(body);

        assertEquals(OK, result.status());
    }
}
