package org.catalyst.slackservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConversationsResponse extends SlackResponse {
    public static class Channel {
        public String id;
        public String name;

        @JsonProperty("is_channel")
        public boolean isChannel;
        @JsonProperty("is_group")
        public boolean isGroup;
        @JsonProperty("is_im")
        public boolean isIm;
        public String locale;
    }

    public Channel channel;
}
