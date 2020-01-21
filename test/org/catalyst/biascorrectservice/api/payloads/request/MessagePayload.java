package org.catalyst.biascorrectservice.api.payloads.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePayload {

    // variable
    @JsonProperty
    private String text;

    // getter
    public String getText() {
        return text;
    }

    // constructor
    private MessagePayload(String text) {
        this.text = text;
    }

    // builder
    public static class MessagePayloadBuilder {

        private String text;

        public MessagePayloadBuilder setText(String text) {
            this.text = text;
            return this;
        }

        public MessagePayload build() {
            return new MessagePayload(text);
        }
    }
}
