package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Attachment {
    public String fallback;
    public String title;
    @JsonProperty("callback_id")
    public String callbackId;
    @JsonProperty("attachment_type")
    public String attachmentType = "default";
    public List<Action> actions;

    public Attachment(String fallback, String title, String callbackId, List<Action> actions) {
        this.fallback = fallback;
        this.title = title;
        this.callbackId = callbackId;
        this.actions = actions;
    }
}
