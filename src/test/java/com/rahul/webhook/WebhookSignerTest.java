package com.rahul.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignerTest {

    @Test
    void samePayloadAndSecretShouldProduceSameSignature() {

        String payload = """
                {
                  "fileId": "123",
                  "status": "COMPLETED"
                }
                """;

        String secret = "test-secret";

        String first = WebhookSigner.sign(payload, secret);

        String second = WebhookSigner.sign(payload, secret);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentPayloadShouldProduceDifferentSignature() {

        String first = WebhookSigner.sign("{\"status\":\"COMPLETED\"}", "test-secret");

        String second = WebhookSigner.sign("{\"status\":\"FAILED\"}", "test-secret");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void differentSecretShouldProduceDifferentSignature() {

        String payload = "{\"status\":\"COMPLETED\"}";

        String first = WebhookSigner.sign(payload, "secret-a");

        String second = WebhookSigner.sign(payload, "secret-b");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void signatureShouldUseSha256Prefix() {

        String signature = WebhookSigner.sign("{\"test\":true}", "secret");

        assertThat(signature).startsWith("sha256=");

        assertThat(signature).hasSize(71);
    }
}