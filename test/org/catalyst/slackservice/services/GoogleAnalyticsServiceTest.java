package org.catalyst.slackservice.services;

import org.junit.Test;
import org.mockito.Mockito;
import play.libs.ws.WSClient;

public class GoogleAnalyticsServiceTest {

    @Test
    public void testTrack() {
        var wsClientMock = Mockito.mock(WSClient.class);
        var key = new AnalyticsKey("trackingId", "channelId", "userId");
        var trigger = "trigger";
        var event = AnalyticsEvent.createMessageEvent(key, trigger);

        var sut = new GoogleAnalyticsService(wsClientMock);

        sut.track(event);

        Mockito.verify(wsClientMock).url("https://google-analytics.com/collect?t=event&v=1&el=&ea=Message+-+Bias+Match&tid=trackingId&ec=channelId&cid=userId");
    }
}
