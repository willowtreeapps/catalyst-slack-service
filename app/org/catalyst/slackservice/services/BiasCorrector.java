package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.CorrectorResponse;
import org.catalyst.slackservice.util.SlackLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import org.catalyst.slackservice.util.AppConfig;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class BiasCorrector implements MessageCorrector, WSBodyReadables {
    private final static String CONTENT_TYPE_JSON = "application/json";
    final Logger logger = LoggerFactory.getLogger(BiasCorrector.class);

    private static class Request {
        public String text;
        public String context;

        public Request(String text, String context) {
            this.text = text;
            this.context = context;
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

    public CompletionStage<String> getCorrection(String input, SlackLocale slackLocale) {
        var bcInput = new Request(input, slackLocale.getCode());

        var request = _wsClient.url(_config.getBiasCorrectUrl())
                .setContentType(CONTENT_TYPE_JSON);

        var jsonPromise = request.post(Json.toJson(bcInput));

        return jsonPromise.thenApplyAsync(r -> {
            if (r.getStatus() != 200) {
                logger.error("corrector service failed. {}", r.getStatus());
                return "";
            }

            var response = Json.fromJson(r.getBody(json()), CorrectorResponse.class);
            return response.input != null && !response.input.equals((response.correction)) ?
                response.correction : "";
            }, _ec.current());
    }
}
