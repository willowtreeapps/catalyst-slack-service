package services;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;

import javax.inject.Inject;
import javax.inject.Named;
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
    private final String _url;

    @Inject
    BiasCorrector(@Named("BIAS_CORRECT_URL") String url, WSClient wsClient) {
        this._wsClient = wsClient;
        this._url = url;
    }

    @Override
    public String getCorrection(String input) {
        var request = new Request();
        request.text = input;

        var jsonPromise = _wsClient.url(_url)
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
