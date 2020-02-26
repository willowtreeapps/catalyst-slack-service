package org.catalyst.slackservice.services;

import com.google.inject.Inject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

public class GoogleAnalyticsService implements AnalyticsService {
   private final Logger logger = LoggerFactory.getLogger(GoogleAnalyticsService.class);
   private final WSClient _wsClient;

   @Inject
   public GoogleAnalyticsService(WSClient wsClient) {
      _wsClient = wsClient;
   }

   public void track(AnalyticsEvent event) {
      try {
         var uri = getURI(event);
         var request = _wsClient.url(uri.toString());
         request.post("");
      }
      catch(Exception e) {
          logger.error("failed to send google analytics event");
      }
   }

   private URI getURI(AnalyticsEvent event) throws URISyntaxException {
      var parameters = new ArrayList<NameValuePair>();
      for (var entry: event.getMap().entrySet()) {
         parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      }

      return new URIBuilder()
              .setScheme("https")
              .setHost("google-analytics.com")
              .setPath("/collect")
              .setParameters(parameters)
              .build();
   }
}
