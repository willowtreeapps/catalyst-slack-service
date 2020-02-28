package org.catalyst.slackservice.services;

import org.catalyst.slackservice.util.SlackLocale;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.util.Arrays;
import java.util.List;

public class GoogleAnalyticsServiceTest {

    @Test
    public void testTrack() {
        // validate that a wsRequest is created with the required url, headers, and query parameters
        var wsClientMock = Mockito.mock(WSClient.class);
        var wsRequestMock = Mockito.mock(WSRequest.class);
        Mockito.when(wsClientMock.url(Mockito.anyString())).thenReturn(wsRequestMock);
        Mockito.when(wsRequestMock.addHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(wsRequestMock);

        var key = new AnalyticsKey("trackingId", "teamId", "teamName", "channelId", "userId", new SlackLocale("en-US"));
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
        Mockito.verify(wsRequestMock, Mockito.times(12))
                .addQueryParameter(keyArgument.capture(), valueArgument.capture());

        Assert.assertEquals(keyArgument.getAllValues(), expectedRequestKeys);
        Assert.assertEquals(valueArgument.getAllValues(), expectedRequestValues);
        Mockito.verify(wsRequestMock, Mockito.times(1)).post("");
    }

    private static List<String> expectedRequestKeys = Arrays.asList(
        "User-Agent",
        "cd2",
        "cd1",
        "cd4",
        "cd3",
        "t",
        "cd5",
        "v",
        "el",
        "ea",
        "tid",
        "ec",
        "cid"
    );

    private static List<String> expectedRequestValues = Arrays.asList(
        "CatalystBiasCorrectService/1.0.1",
        "teamId",
        "teamName",
        "userId",
        "channelId",
        "event",
        "en-US",
        "1",
        "",
        "Message - Bias Match",
        "trackingId",
        "teamId",
        "userId"
    );
}
