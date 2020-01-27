package domain;

import java.util.List;

public class Message {

    public String channel;
    public String token;
    public String user;
    public String text;
    public List<Attachment> attachments;
    public String ts;
}
