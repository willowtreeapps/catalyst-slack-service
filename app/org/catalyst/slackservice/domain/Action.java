package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Action {
    @JsonIgnore
    public static String YES = "yes";
    @JsonIgnore
    public static String NO = "no";
    @JsonIgnore
    public static String LEARN_MORE = "learn_more";

    public String name;
    public String text;
    public String type = "button";
    public String value;
    public String style;
    public String url;

    public Action(){}
    public Action(String name, String text, String value, String style, String url) {
        this.name = name;
        this.text = text;
        this.value = value;
        this.style = style;
        this.url = url;
    }
}
