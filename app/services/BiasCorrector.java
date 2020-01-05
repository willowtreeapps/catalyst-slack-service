package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

public class BiasCorrector implements MessageCorrector,WSBodyReadables {
    private static class Response {
        public String input;
        public String context;
        public String correction;
    }

    private static class Request {
        public String text;
    }

    private final WSClient _wsClient;
    private final AppConfig _config;

    @Inject
    BiasCorrector(AppConfig config, WSClient wsClient) {
        this._wsClient = wsClient;
        this._config = config;
    }

    @Override
    public String getCorrection(String input) {
        var request = new Request();
        request.text = input;

        var jsonPromise = _wsClient.url(_config.getBiasCorrectUrl())
                .setContentType("application/json")
                .post(Json.toJson(request))
                .thenApply(r -> r.getBody(json()));

        JsonNode response = null;
        try {
            response = jsonPromise.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return response != null ? Json.fromJson(response, Response.class).correction : "";
    }
}
