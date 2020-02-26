package org.catalyst.slackservice.services;

import play.libs.ws.WSResponse;

import java.util.concurrent.CompletionStage;

public interface AnalyticsService {
   CompletionStage<WSResponse> track(AnalyticsEvent event);
}
