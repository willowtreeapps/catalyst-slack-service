package services;

import domain.CorrectorResponse;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class BiasCorrector implements MessageCorrector, WSBodyReadables {

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

        var request = _wsClient.url(_config.getBiasCorrectUrl())
                .setContentType("application/json");

        var jsonPromise = request.post(Json.toJson(bcInput));

        return jsonPromise.thenApplyAsync(r -> {
            var response = Json.fromJson(r.getBody(json()), CorrectorResponse.class);
            return response.input != null && !response.input.equals((response.correction)) ?
                response.correction : "";
            }, _ec.current());
    }
}
