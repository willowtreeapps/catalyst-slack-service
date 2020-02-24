package org.catalyst.slackservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ResultHelper {
    public static CompletionStage<Result> badRequest(MessageHandler messages, String error) {
        return badRequest(messages.get(error));
    }

    public static CompletionStage<Result> badRequest(String error) {
        return CompletableFuture.completedFuture(Controller.badRequest( Json.toJson(Map.of(
                "ok", Boolean.valueOf(false),
                "error", error))));
    }

    public static CompletionStage<Result> noContent() {
        return CompletableFuture.completedFuture(Controller.noContent());
    }

    public static CompletionStage<Result> ok(JsonNode json) {
        return CompletableFuture.completedFuture(Controller.ok(json));
    }

    public static CompletionStage<Result> ok() {
        return ok(Json.toJson(Map.of("ok", Boolean.valueOf(true))));
    }
}
