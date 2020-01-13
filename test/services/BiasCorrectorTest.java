package services;

import domain.CorrectorResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import util.MockConfig;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static play.mvc.Results.ok;

public class BiasCorrectorTest {
    private WSClient wsClient;
    private Server server;
    private MockConfig config;
    private BiasCorrector service;
    @Test
    public void testCorrect() throws Exception {
        var r = service.getCorrection("she's fabulous");
        var x = r.toCompletableFuture().get(10, TimeUnit.SECONDS);
        System.out.println(x);
    }

    @Before
    public void setup() {
        config = new MockConfig();

        var response = new CorrectorResponse();
        response.input = "she's fabulous";
        response.correction = "";

        server = Server.forRouter( (components) -> RoutingDsl
                .fromComponents(components)
                .POST(config.getBiasCorrectUrl())
                .routingTo( request ->
                        ok(Json.toJson(response))
                )
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
