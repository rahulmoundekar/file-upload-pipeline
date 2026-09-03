package com.rahul.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class WebhookSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSigner() {
    }

    public static String sign(String payload, String secret) {

        try {

            Mac mac = Mac.getInstance(ALGORITHM);

            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            mac.init(key);

            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);

            for (byte b : digest) {

                hex.append(String.format("%02x", b));
            }

            return "sha256=" + hex;

        } catch (Exception e) {

            throw new WebhookDeliveryException("Unable to sign webhook payload", e);
        }
    }
}