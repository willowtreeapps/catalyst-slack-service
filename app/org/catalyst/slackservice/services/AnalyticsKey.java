package org.catalyst.slackservice.services;

public class AnalyticsKey {
    public String trackingId;
    public String channelId;
    public String userId;

    public AnalyticsKey(String trackingId, String channelId, String userId) {
        this.trackingId = trackingId;
        this.channelId = channelId;
        this.userId = userId;
    }
}

