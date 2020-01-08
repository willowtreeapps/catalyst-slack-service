package domain;

import java.util.List;

public class Attachment {
    public String fallback;
    public String title;
    public String callback_id;
    public String attachment_type = "default";
    public List<Action> actions;

    public Attachment(String fallback, String title, String callbackId, List<Action> actions) {
        this.fallback = fallback;
        this.title = title;
        this.callback_id = callbackId;
        this.actions = actions;
    }
}
