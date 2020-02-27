package org.catalyst.slackservice.services;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.util.Arrays;

public class GoogleAnalyticsServiceTest {

    @Test
    public void testTrack() {
        // validate that a wsRequest is created with the required url, headers, and query parameters
        var wsClientMock = Mockito.mock(WSClient.class);
        var wsRequestMock = Mockito.mock(WSRequest.class);
        Mockito.when(wsClientMock.url(Mockito.anyString())).thenReturn(wsRequestMock);
        Mockito.when(wsRequestMock.addHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(wsRequestMock);

        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var trigger = "trigger";
        var event = AnalyticsEvent.createMessageEvent(key, trigger);

        var sut = new GoogleAnalyticsService(wsClientMock);

        sut.track(event);

        Mockito.verify(wsClientMock, Mockito.times(1))
                .url("https://google-analytics.com/collect");

        var keyArgument = ArgumentCaptor.forClass(String.class);
        var valueArgument = ArgumentCaptor.forClass(String.class);

        Mockito.verify(wsRequestMock, Mockito.times(1))
                .addHeader(keyArgument.capture(), valueArgument.capture());
        Mockito.verify(wsRequestMock, Mockito.times(7))
                .addQueryParameter(keyArgument.capture(), valueArgument.capture());

        Assert.assertEquals(keyArgument.getAllValues(),
                Arrays.asList("User-Agent", "t", "v", "el", "ea", "tid", "ec", "cid"));
        Assert.assertEquals(valueArgument.getAllValues(),
                Arrays.asList("CatalystBiasCorrectService/1.0.1", "event", "1", "", "Message - Bias Match", "trackingId", "channelId", "userId"));

        Mockito.verify(wsRequestMock, Mockito.times(1)).post("");
    }
}
