package org.catalyst.slackservice.services;

import org.catalyst.slackservice.util.SlackLocale;

import java.util.concurrent.CompletionStage;

public interface MessageCorrector {

    CompletionStage<String> getCorrection(String input, SlackLocale locale);
}
