package org.catalyst.slackservice.services;

import com.google.inject.Inject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import play.libs.ws.WSClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class GoogleAnalyticsService implements AnalyticsService {

   private final WSClient _wsClient;

   @Inject
   public GoogleAnalyticsService(WSClient wsClient) {
      _wsClient = wsClient;
   }

   public void track(AnalyticsEvent event) {
      try {
         var uri = getURI(event);
         System.out.println(uri.toString());
         var request = _wsClient.url(uri.toString())
                 .setContentType("application/json");

         // TODO: make async
          var response = request.post("");
      }
      catch(Exception e) {
          // TODO: handle exceptions
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
