package org.catalyst.slackservice.services;

import org.catalyst.slackservice.util.SlackLocale;

public class AnalyticsKey {
    public String trackingId;
    public String teamId;
    public String teamName;
    public String channelId;
    public String userId;
    public SlackLocale locale;

    public AnalyticsKey(String trackingId, String teamId, String teamName, String channelId, String userId, SlackLocale locale) {
        this.trackingId = trackingId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.channelId = channelId;
        this.userId = userId;
        this.locale = locale;
    }
}

