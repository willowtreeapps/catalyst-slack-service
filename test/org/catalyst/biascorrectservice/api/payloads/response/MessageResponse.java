package org.catalyst.biascorrectservice.api.payloads.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageResponse {

    // variables

    @JsonProperty
    private String input;
    @JsonProperty
    private String context;
    @JsonProperty
    private String correction;

    // setters

    public void setInput(String input) {
        this.input = input;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setCorrection(String correction) {
        this.correction = correction;
    }

    // getters

    public String getInput() {
        return input;
    }

    public String getContext() {
        return context;
    }

    public String getCorrection() {
        return correction;
    }

}
