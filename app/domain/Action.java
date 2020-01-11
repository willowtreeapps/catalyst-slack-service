package domain;

public class Action {
    public String name;
    public String text;
    public String type = "button";
    public String value;
    public String style;

    public Action(String name, String text, String value, String style) {
        this.name = name;
        this.text = text;
        this.value = value;
        this.style = style;
    }
}
