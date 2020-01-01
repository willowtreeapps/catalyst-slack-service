package domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestAction {
    private String _token;
    private String _challenge;
    private String _type;

    @JsonProperty("token")
    public String getToken() {
        return _token;
    }

    @JsonProperty("challenge")
    public String getChallenge() {
        return _challenge;
    }

    @JsonProperty("type")
    public String getType() {
        return _type;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        _token = token;
    }

    @JsonProperty("challenge")
    public void setChallenge(String challenge) {
        _challenge = challenge;
    }

    @JsonProperty("type")
    public void setType(String type) {
        _type = type;
    }
//    {"token":"rQwsoMG3f3srS4gGWz3r8ha9","challenge":"LJ6Lw0EB95l5AP85Sqh0TRKMcwjxHSnFAVqfdSuZ8MQGwc8vo7BZ","type":"url_verification"}
}
