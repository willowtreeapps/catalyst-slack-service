package domain;

import java.util.List;

public class Message {
    public String ok = "true";
    public String channel;
    public String token;
    public String user;
    public String as_user = "false";
    public String text;
    public List<Attachment> attachments;

    public Message(String channel, String token, String user, String text, List<Attachment> attachments) {
        this.channel = channel;
        this.token = token;
        this.user = user;
        this.text = text;
        this.attachments = attachments;
    }
}
