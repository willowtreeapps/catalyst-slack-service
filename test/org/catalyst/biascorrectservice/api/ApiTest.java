package org.catalyst.biascorrectservice.api;

import org.catalyst.biascorrectservice.api.payloads.response.MessageResponse;
import org.catalyst.biascorrectservice.api.requests.Correct;
import org.junit.Ignore;
import org.junit.Test;
import org.catalyst.biascorrectservice.api.payloads.request.MessagePayload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApiTest {

    @Test
    public void testBiasedEnglishMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("she is vain")
                .setContext("en")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("she is neat")));
    }

    @Test
    public void testSafeEnglishMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("she is smart")
                .setContext("en")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("she is smart")));
    }

    @Test@Ignore
    public void testBiasedSpanishMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("ella es hostil")
                .setContext("es")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("ella es firme")));
    }

    @Test@Ignore
    public void testSafeSpanishMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("ella es fabulosa")
                .setContext("es")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("ella es fabulosa")));
    }

    @Test@Ignore
    public void testBiasedGermanMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("sie ist barsch")
                .setContext("de")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("sie ist selbstbewusst")));
    }

    @Test@Ignore
    public void testSafeGermanMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("sie ist fabelhaft")
                .setContext("es")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("Sie ist fabelhaft")));
    }

    @Test@Ignore
    public void testBiasedFrenchMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("elle est émotive")
                .setContext("fr")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("elle est passionnée")));
    }

    @Test@Ignore
    public void testSafeFrenchMessage() {
        MessagePayload payload = new MessagePayload.MessagePayloadBuilder()
                .setText("elle est fabuleuse")
                .setContext("fr")
                .build();

        ResponseEntity<MessageResponse> messageResponse = Correct.postCorrect(payload);

        assertThat(messageResponse.getStatusCode(), is(HttpStatus.OK));
        assertThat(messageResponse.getBody().getCorrection(), containsString(String.valueOf("elle est fabuleuse")));
    }
}
