package org.catalyst.biascorrectservice.api.requests;

import org.catalyst.biascorrectservice.api.payloads.request.MessagePayload;
import org.catalyst.biascorrectservice.api.payloads.response.MessageResponse;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class Correct {

    private static RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    public static ResponseEntity<MessageResponse> postCorrect(MessagePayload payload) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MessagePayload> httpEntity = new HttpEntity<>(payload, requestHeaders);

        return restTemplate.exchange("http://localhost:9000/corrector/correct", HttpMethod.POST, httpEntity, MessageResponse.class);
    }

}
