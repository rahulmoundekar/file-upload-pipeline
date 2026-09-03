package com.rahul.webhook;

import com.rahul.config.WebhookProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookClientTest {

    private HttpServer server;

    private AtomicReference<String> receivedPayload;
    private AtomicReference<String> receivedSignature;

    @BeforeEach
    void setUp() throws Exception {

        receivedPayload = new AtomicReference<>();

        receivedSignature = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/webhook", exchange -> {

            byte[] body = exchange.getRequestBody().readAllBytes();

            receivedPayload.set(new String(body));

            receivedSignature.set(exchange.getRequestHeaders().getFirst("X-Webhook-Signature"));

            exchange.sendResponseHeaders(200, -1);

            exchange.close();
        });

        server.start();
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendWebhook() {

        int port = server.getAddress().getPort();

        WebhookProperties properties = new WebhookProperties(true, "http://localhost:" + port + "/webhook", "test-secret", 5000, 5000);

        WebhookClient client = new WebhookClient(properties);

        String payload = "{\"status\":\"COMPLETED\"}";

        String signature = WebhookSigner.sign(payload, "test-secret");

        client.send(payload, signature);

        assertThat(receivedPayload.get()).isEqualTo(payload);

        assertThat(receivedSignature.get()).isEqualTo(signature);
    }
}