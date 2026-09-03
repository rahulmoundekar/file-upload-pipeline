package com.rahul.integration;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.rahul.event.EventSerializer;
import com.rahul.event.FileCompletedEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class WebhookWorkerIntegrationTest {

    private static final String TOPIC = "file.completed";

    private static final int PARTITIONS = 3;

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    static WireMockServer wireMockServer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventSerializer eventSerializer;

    @BeforeAll
    static void startWireMock() throws Exception {

        wireMockServer = new WireMockServer(0);

        wireMockServer.start();

        createTopic();
    }

    @AfterAll
    static void stopWireMock() {

        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.producer.enabled", () -> true);

        registry.add("kafka.consumer.enabled", () -> true);

        registry.add("kafka.consumer.webhook-group", () -> "webhook-integration-" + UUID.randomUUID());

        registry.add("kafka.topics.file-completed", () -> TOPIC);

        registry.add("webhook.enabled", () -> true);

        registry.add("webhook.url", () -> "http://localhost:" + wireMockServer.port() + "/webhooks/files");

        registry.add("webhook.secret", () -> "integration-test-secret");

        registry.add("webhook.connect-timeout-ms", () -> 5000);

        registry.add("webhook.read-timeout-ms", () -> 10000);
    }

    @Test
    void completedEventShouldBeDeliveredToWebhook() throws Exception {

        wireMockServer.resetAll();

        UUID eventId = UUID.randomUUID();

        UUID fileId = UUID.randomUUID();

        FileCompletedEvent event = new FileCompletedEvent(eventId, fileId, "COMPLETED", "uploads/test.jpg", "test.jpg", "image/jpeg", 12345L, "a".repeat(64), Instant.now());

        String payload = eventSerializer.serialize(event);

        kafkaTemplate.send(TOPIC, fileId.toString(), payload).get();

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {

            wireMockServer.verify(postRequestedFor(urlEqualTo("/webhooks/files")));
        });

        List<com.github.tomakehurst.wiremock.stubbing.ServeEvent> requests = wireMockServer.getAllServeEvents();

        assertThat(requests).hasSize(1);

        var request = requests.get(0).getRequest();

        assertThat(request.getMethod()).isEqualTo(com.github.tomakehurst.wiremock.http.RequestMethod.POST);

        assertThat(request.getHeader("Content-Type")).contains(MediaType.APPLICATION_JSON_VALUE);

        assertThat(request.getHeader("X-Webhook-Signature")).startsWith("sha256=");

        assertThat(request.getBodyAsString()).isEqualTo(payload);
    }

    private static void createTopic() throws Exception {

        Map<String, Object> properties = new HashMap<>();

        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {

            try {

                adminClient.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) 1))).all().get();

            } catch (Exception ignored) {
                // Topic may already exist.
            }
        }
    }
}