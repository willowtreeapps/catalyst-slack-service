package org.catalyst.slackservice.services;

import java.util.concurrent.CompletionStage;

public interface MessageCorrector {

    CompletionStage<String> getCorrection(String input);
}
