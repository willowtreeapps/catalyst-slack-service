package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.Action;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsEvent {
    private static final String version = "1";
    private static final String type = "event";
    private String trackingId;
    private String userId;
    private String category;
    private String action;
    private String label;

    private AnalyticsEvent(AnalyticsKey key, String action) {
        this.trackingId = key.trackingId;
        this.userId = key.userId;
        this.category = key.channelId;
        this.action = action;
    }

    private AnalyticsEvent(AnalyticsKey key, String action, String label) {
        this(key, action);
        this.label = label;
    }

    public Map<String, String> getMap() {
        // TODO: should empty strings be included, should that be handled here or in the service?
        return new HashMap<>() {{
            put("v", version);
            put("t", type);
            put("tid", trackingId);
            put("cid", userId);
            put("ec", category);
            put("ea", action);
            put("el", label);
        }};
    }

    public static AnalyticsEvent createMessageEvent(AnalyticsKey key, String correction)  {
        var action = correction != null && correction.length() > 0 ? "Message - Bias Match" : "Message - No Action";
        return new AnalyticsEvent(key, action, "");
    }

    public static AnalyticsEvent createMessageActionEvent(AnalyticsKey key, Action action) {
        String analyticsAction = "";
        if (action.value.equals(Action.YES)) {
            analyticsAction = "User - Applied Suggestion";
        }
        else if (action.value.equals(Action.NO)) {
            analyticsAction = "User - Rejected Suggestion";
        }
        else if (action.value.equals(Action.LEARN_MORE)) {
            analyticsAction = "User - Clicked Learn More";
        }

        return new AnalyticsEvent(key, analyticsAction, "");
    }
}
