package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.Action;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsEvent {
    private static final String MESSAGE_BIAS_MATCH = "Message - Bias Match";
    private static final String MESSAGE_NO_ACTION = "Message - No Action";
    private static final String USER_APPLIED_SUGGESTION = "User - Applied Suggestion";
    private static final String USER_REJECTED_SUGGESTION = "User - Rejected Suggestion";
    private static final String USER_CLICKED_LEARN_MORE = "User - Clicked Learn More";
    private static final String version = "1";
    private static final String type = "event";
    private String trackingId;
    private String userId;
    private String category;
    private String action;
    private String label;
    private String cd1;
    private String cd2;
    private String cd3;
    private String cd4;
    private String cd5;

    private AnalyticsEvent(AnalyticsKey key, String action) {
        this.trackingId = key.trackingId;
        this.userId = key.userId;
        this.category = key.teamId;
        this.action = action;
        this.cd1 = key.teamName;
        this.cd2 = key.teamId;
        this.cd3 = key.channelId;
        this.cd4 = key.userId;
        this.cd5 = key.locale.getCode();
    }

    private AnalyticsEvent(AnalyticsKey key, String action, String label) {
        this(key, action);
        this.label = label;
    }

    public Map<String, String> getMap() {
        return new HashMap<>() {{
            put("v", version);
            put("t", type);
            put("tid", trackingId);
            put("cid", userId);
            put("ec", category);
            put("ea", action);
            put("el", label);
            put("cd1", cd1);
            put("cd2", cd2);
            put("cd3", cd3);
            put("cd4", cd4);
            put("cd5", cd5);
        }};
    }

    public static AnalyticsEvent createMessageEvent(AnalyticsKey key, String correction)  {
        var action = correction != null && correction.length() > 0 ? MESSAGE_BIAS_MATCH : MESSAGE_NO_ACTION;
        return new AnalyticsEvent(key, action, "");
    }

    public static AnalyticsEvent createMessageActionEvent(AnalyticsKey key, Action action) throws IllegalArgumentException {
        String analyticsAction = "";
        if (action.value != null && action.value.equals(Action.YES)) {
            analyticsAction = USER_APPLIED_SUGGESTION;
        }
        else if (action.value != null && action.value.equals(Action.NO)) {
            analyticsAction = USER_REJECTED_SUGGESTION;
        }
        else if (action.value != null && action.value.equals(Action.LEARN_MORE)) {
            analyticsAction = USER_CLICKED_LEARN_MORE;
        }
        else {
            throw new IllegalArgumentException();
        }

        return new AnalyticsEvent(key, analyticsAction, "");
    }
}
