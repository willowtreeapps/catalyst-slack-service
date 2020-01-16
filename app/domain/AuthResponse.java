package domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {
    public boolean ok;
    public String error;
    @JsonProperty("team_id")
    public String teamId;
    @JsonProperty("user_id")
    public String userId;
    @JsonProperty("access_token")
    public String userToken;
}
