package org.catalyst.biascorrectservice.api;

import org.catalyst.biascorrectservice.api.payloads.response.MessageResponse;
import org.catalyst.biascorrectservice.api.requests.Correct;
import org.junit.Test;
import org.catalyst.biascorrectservice.api.payloads.request.MessagePayload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApiTest {

    @Test
    public void testValidBiasCorrection() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("she is vain")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("she is neat")));

    }

}
