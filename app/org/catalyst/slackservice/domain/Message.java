package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Message {

    public String channel;
    public String token;
    public String user;
    public String text;
    public List<Attachment> attachments;
    public String ts;
    @JsonProperty("thread_ts")
    public String threadTs;
}
