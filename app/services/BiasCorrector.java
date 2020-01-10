package services;

import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class BiasCorrector implements MessageCorrector, WSBodyReadables {
    private static class Response {
        public String input;
        public String context;
        public String correction;
    }

    private static class Request {
        public String text;

        public Request(String text) {
            this.text = text;
        }
    }

    private final HttpExecutionContext _ec;
    private final WSClient _wsClient;
    private final AppConfig _config;

    @Inject
    BiasCorrector(HttpExecutionContext ec, AppConfig config, WSClient wsClient) {
        this._ec = ec;
        this._wsClient = wsClient;
        this._config = config;
    }

    public CompletionStage<String> getCorrection(String input) {
        var bcInput = new Request(input);

        return _wsClient.url(_config.getBiasCorrectUrl())
            .setContentType("application/json")
            .post(Json.toJson(bcInput))
            .thenApplyAsync(r -> {
                var json = r.getBody(json());
                var response = Json.fromJson(json, Response.class);
                return !response.input.equals(response.correction) ? response.correction : "";

            }, _ec.current());
    }
}
