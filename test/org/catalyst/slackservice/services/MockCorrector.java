package org.catalyst.slackservice.services;

import org.catalyst.slackservice.util.SlackLocale;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockCorrector implements MessageCorrector {

    @Override
    public CompletionStage<String> getCorrection(String input, SlackLocale slackLocale) {
        return CompletableFuture.completedFuture(input.equals("she's so quiet") ? "she's so thoughtful" : "");
    }
}
