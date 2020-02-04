package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    @JsonProperty("client_msg_id")
    public String clientMsgId;
    public String type;
    public String subtype;
    public String text;
    public String user;
    public String ts;
    public String team;
    public String username;
    @JsonProperty("bot_id")
    public String botId;
    public String channel;
    @JsonProperty("event_ts")
    public String eventTs;
    @JsonProperty("channel_type")
    public String channelType;
    @JsonProperty("thread_ts")
    public String threadTs;
}
