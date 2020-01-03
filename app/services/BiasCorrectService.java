package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import util.AppConfig;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

public class BiasCorrectService implements WSBodyReadables {

    public static class CorrectRequest {
        public String text;
    }

    public static class CorrectResponse {
        public String context;
        public String correction;
        public String input;
    }
    private WSClient _wsClient;
    private AppConfig _config;

    @Inject
    BiasCorrectService(AppConfig config, WSClient wsClient) {
        this._wsClient = wsClient;
        this._config = config;
    }

    public BiasCorrectService.CorrectResponse correct(BiasCorrectService.CorrectRequest text) {
        var jsonPromise = _wsClient.url(_config.getBiasCorrectUrl())
                .setContentType("application/json")
                .post(Json.toJson(text))
                .thenApply(r -> r.getBody(json()));

        JsonNode response = null;
        try {
            response = jsonPromise.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return Json.fromJson(response, BiasCorrectService.CorrectResponse.class);
    }
}
