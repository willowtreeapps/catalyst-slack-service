package domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Message {
    public boolean ok = true;
    public String channel;
    public String token;
    public String user;
    @JsonProperty("as_user")
    public boolean asUser = false;
    public String text;
    @JsonProperty("trigger_id")
    public String triggerId;
    public String ts;
    public List<Attachment> attachments;

    public Message(String channel, String token, String user, String text, List<Attachment> attachments) {
        this.channel = channel;
        this.token = token;
        this.user = user;
        this.text = text;
        this.attachments = attachments;
    }
}
