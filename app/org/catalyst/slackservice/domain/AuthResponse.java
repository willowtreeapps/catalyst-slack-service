package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse extends SlackResponse {
    public static class AuthedUser {
        public String id;
        @JsonProperty("access_token")
        public String accessToken;
    }

    public static class Team {
        public String id;
        public String name;
    }

    @JsonProperty("authed_user")
    public AuthedUser user;
    public Team team;

    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("access_token")
    public String accessToken;
    @JsonProperty("bot_user_id")
    public String botUserId;
}
