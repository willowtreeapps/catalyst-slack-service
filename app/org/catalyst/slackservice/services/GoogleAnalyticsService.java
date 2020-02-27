package org.catalyst.slackservice.services;

import com.google.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class GoogleAnalyticsService implements AnalyticsService {
   private final Logger logger = LoggerFactory.getLogger(GoogleAnalyticsService.class);
   private final WSClient _wsClient;

   @Inject
   public GoogleAnalyticsService(WSClient wsClient) {
      _wsClient = wsClient;
   }

   public void track(AnalyticsEvent event) {
      try {
         var uri = getURI();
         var request = _wsClient.url(uri.toString())
                 .addHeader("User-Agent", "CatalystBiasCorrectService/1.0.1");
         request = addQueryParameters(request, event.getMap()); // addQueryParameters will url encode the values
         request.post("");
      }
      catch(Exception e) {
          logger.error("failed to send google analytics event");
      }
   }

   private WSRequest addQueryParameters(WSRequest request, Map<String, String> values) {
      for (var entry: values.entrySet()) {
         request.addQueryParameter(entry.getKey(), entry.getValue()); // addQueryParmeter will url encode the value
      }

      return request;
   }

   private URI getURI() throws URISyntaxException {
      return new URIBuilder()
              .setScheme("https")
              .setHost("google-analytics.com")
              .setPath("/collect")
              .build();
   }
}
