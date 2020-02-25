package org.catalyst.slackservice.services;

public class AnalyticsKey {
    public String trackingId; // TODO: not sure if this belongs in this class
    public String teamId;   // TODO: can this be removed?
    public String channelId;
    public String userId;

    public AnalyticsKey(String trackingId, String channelId, String userId) {
        this.trackingId = trackingId;
        this.channelId = channelId;
        this.userId = userId;
    }
}

