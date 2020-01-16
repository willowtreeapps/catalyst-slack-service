package controllers;

import db.TokenHandler;
import db.MockDbHandler;
import org.junit.Test;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
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
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.route;

public class HomeControllerTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(bind(AppConfig.class).to(MockConfig.class))
                .overrides(bind(MessageCorrector.class).to(MockCorrector.class))
                .overrides(bind(AppService.class).to(MockSlackService.class))
                .overrides(bind(TokenHandler.class).to(MockDbHandler.class))
                .build();
    }

    @Test
    public void testHealthCheckPage() {
        var request = new Http.RequestBuilder()
                .method(GET)
                .uri("/bias-correct/v2/slack");

        var result = route(app, request);
        assertEquals(OK, result.status());
    }
}
