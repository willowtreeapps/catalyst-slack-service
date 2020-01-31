package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.CorrectorResponse;
import org.catalyst.slackservice.util.SlackLocale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import org.catalyst.slackservice.util.MockConfig;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

public class BiasCorrectorTest {
    private WSClient wsClient;
    private Server server;
    private MockConfig config = new MockConfig();
    private BiasCorrector service;

    @Test
    public void testCorrect() throws Exception {
        setupSuccessful();
        var correction = service.getCorrection("she's fabulous", new SlackLocale())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        tearDown();
        Assert.assertEquals("", correction);
    }

    @Test
    public void testCorrectorServiceUnavailable() throws Exception {
        setupFailed();
        var correction = service.getCorrection("she's fabulous", new SlackLocale())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        tearDown();
        Assert.assertEquals("", correction);
    }

    public void setupSuccessful() {

        var response = new CorrectorResponse();
        response.input = "she's fabulous";
        response.correction = "";

        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)
                .POST(config.getBiasCorrectUrl())
                .routingTo( request -> ok(Json.toJson(response)))
                .build());

        wsClient = play.test.WSTestClient.newClient(server.httpPort());
        var ec = new HttpExecutionContext(ForkJoinPool.commonPool());

        service = new BiasCorrector(ec, config, wsClient);
    }

    public void setupFailed() {
        var response = new CorrectorResponse();
        response.input = "she's fabulous";
        response.correction = "";

        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)
                .POST(config.getBiasCorrectUrl())
                .routingTo( request -> internalServerError())
                .build());

        wsClient = play.test.WSTestClient.newClient(server.httpPort());
        var ec = new HttpExecutionContext(ForkJoinPool.commonPool());

        service = new BiasCorrector(ec, config, wsClient);
    }

    @After
    public void tearDown() throws IOException {
        try {
            wsClient.close();
        } finally {
            server.stop();
        }
    }
}
