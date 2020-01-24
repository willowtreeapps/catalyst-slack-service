package domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SlackResponse {
    public boolean ok;
    @JsonProperty("message_ts")
    public String messageTs;
    public String warning;
    public String error;
}
