package com.rahul.webhook;

import com.rahul.config.WebhookProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebhookProperties properties;

    public void send(String payload, String signature) {

        RestClient client = RestClient.builder().build();

        try {

            ResponseEntity<Void> response = client.post().uri(properties.url()).contentType(MediaType.APPLICATION_JSON).header("X-Webhook-Signature", signature).body(payload).retrieve().toBodilessEntity();

            if (!response.getStatusCode().is2xxSuccessful()) {

                throw new WebhookDeliveryException("Webhook returned HTTP " + response.getStatusCode().value());
            }

        } catch (WebhookDeliveryException e) {

            throw e;

        } catch (Exception e) {

            throw new WebhookDeliveryException("Unable to deliver webhook", e);
        }
    }
}