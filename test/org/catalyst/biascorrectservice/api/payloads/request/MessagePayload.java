package org.catalyst.biascorrectservice.api.payloads.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePayload {

    // variables
    @JsonProperty
    private String text;
    @JsonProperty
    private String context;

    // getters
    public String getText() {
        return text;
    }

    public String getContext() {
        return context;
    }

    // constructor
    private MessagePayload(String text, String context) {
        this.text = text;
        this.context = context;
    }

    // builder
    public static class MessagePayloadBuilder {

        private String text;
        private String context;

        public MessagePayloadBuilder setText(String text) {
            this.text = text;
            return this;
        }

        public MessagePayloadBuilder setContext(String context) {
            this.context = context;
            return this;
        }

        public MessagePayload build() {
            return new MessagePayload(text, context);
        }
    }
}
