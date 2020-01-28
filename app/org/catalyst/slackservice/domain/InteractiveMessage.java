package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class InteractiveMessage {
    public static class Team {
        public String id;
        public String domain;
    }

    public static class Channel {
        public String id;
        public String name;
    }

    public static class User {
        public String id;
        public String name;
    }

    public String type;
    public List<Action> actions;
    @JsonProperty("callback_id")
    public String callbackId;
    public Team team;
    public Channel channel;
    public User user;
    @JsonProperty("action_ts")
    public String actionTs;
    @JsonProperty("attachment_id")
    public String attachmentId;
    public String token;
    @JsonProperty("is_app_unfurl")
    public boolean isAppUnfurl;
    @JsonProperty("response_url")
    public String responseUrl;
    @JsonProperty("trigger_id")
    public String triggerId;
}
