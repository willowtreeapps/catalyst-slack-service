package controllers;

import db.DbManager;
import db.MockDbManager;
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
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.*;

public class AuthControllerTest extends WithApplication {

    private MockDbManager dbManager = new MockDbManager();
    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(DbManager.class).toInstance(dbManager))
                .build();
    }

    @Test
    public void testMissingRequestCode() {
        var request = new Http.RequestBuilder()
                .method(GET)
                .uri("/bias-correct/v2/slack/auth/redirect");

        var result = route(app, request);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("Missing request code", error);
    }

    @Test
    public void testSlackAuthFailed() {
        var requestCode = "invalid_request_1234";

        var request = new Http.RequestBuilder()
                .method(GET)
                .uri("/bias-correct/v2/slack/auth/redirect?code="+requestCode);

        var result = route(app, request);
        var body = contentAsBytes(result).toArray();
        var error = Json.parse(body).path("error").textValue();

        assertEquals(BAD_REQUEST, result.status());
        assertEquals("request code already used", error);
    }

    @Test
    public void testSlackAuthSucceeded() {
        var requestCode = "valid_request_5678";
        var request = new Http.RequestBuilder()
                .method(GET)
                .uri("/bias-correct/v2/slack/auth/redirect?code="+requestCode);

        var result = route(app, request);

        assertEquals(OK, result.status());
        assertEquals("xoxp-token-123", dbManager.getUserToken("TEAM234", "USER123"));
    }

    @Test
    public void testSigninUrl() {
        var request = new Http.RequestBuilder()
                .method(GET)
                .uri("/bias-correct/v2/slack/signin");

        var result = route(app, request);
        assertEquals(FOUND, result.status());
    }
}
