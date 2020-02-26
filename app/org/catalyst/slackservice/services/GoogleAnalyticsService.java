package org.catalyst.slackservice.services;

import com.google.inject.Inject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

public class GoogleAnalyticsService implements AnalyticsService {

   private final WSClient _wsClient;

   @Inject
   public GoogleAnalyticsService(WSClient wsClient) {
      _wsClient = wsClient;
   }

   public CompletionStage<WSResponse> track(AnalyticsEvent event) {
      try {
         var uri = getURI(event);
         var request = _wsClient.url(uri.toString());
         return request.post("");
      }
      catch(Exception e) {
          // TODO: handle exceptions
      }

      return null;
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
