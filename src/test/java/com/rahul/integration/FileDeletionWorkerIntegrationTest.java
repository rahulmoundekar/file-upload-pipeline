package com.rahul.integration;

import com.rahul.entity.DerivativeType;
import com.rahul.entity.FileDerivative;
import com.rahul.entity.FileMetadata;
import com.rahul.entity.FileStatus;
import com.rahul.entity.ScanStatus;
import com.rahul.entity.ThumbnailStatus;
import com.rahul.event.FileDeletedEvent;
import com.rahul.repository.FileDerivativeRepository;
import com.rahul.repository.FileMetadataRepository;
import com.rahul.repository.ProcessedEventRepository;
import com.rahul.storage.ObjectStorage;
import com.rahul.worker.WorkerNames;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FileDeletionWorkerIntegrationTest {
    private static final String BUCKET = "file-deletion-test";
    private static final String TOPIC = "file-deleted";

    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17").withDatabaseName("file_upload").withUsername("file_app").withPassword("root");
    @Container static MinIOContainer minio = new MinIOContainer("minio/minio:latest");
    @Container static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl); r.add("spring.datasource.username", postgres::getUsername); r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update"); r.add("spring.flyway.enabled", () -> true); r.add("spring.flyway.locations", () -> "classpath:db/migration");
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers); r.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("kafka.producer.enabled", () -> true); r.add("kafka.consumer.enabled", () -> true); r.add("kafka.consumer.deletion-group", () -> "file-deletion-it-" + UUID.randomUUID());
        r.add("kafka.topics.file-deleted", () -> TOPIC); r.add("storage.endpoint", minio::getS3URL); r.add("storage.access-key", minio::getUserName); r.add("storage.secret-key", minio::getPassword); r.add("storage.bucket", () -> BUCKET); r.add("storage.secure", () -> false); r.add("storage.bucket-initializer.enabled", () -> true); r.add("outbox.publisher.enabled", () -> false); r.add("webhook.enabled", () -> false);
    }
    @Autowired KafkaTemplate<String,String> kafkaTemplate;
    @Autowired ObjectStorage objectStorage;
    @Autowired FileMetadataRepository files;
    @Autowired FileDerivativeRepository derivatives;
    @Autowired ProcessedEventRepository processed;
    @Autowired ObjectMapper objectMapper;
    @BeforeEach void topic(){try(AdminClient a=AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,kafka.getBootstrapServers()))){if(!a.listTopics().names().get().contains(TOPIC))a.createTopics(List.of(new NewTopic(TOPIC,3,(short)1))).all().get();}catch(Exception e){throw new IllegalStateException(e);}}

    @Test void deletionEventShouldDeleteObjectsAndMarkFileDeleted() throws Exception {
        UUID eventId=UUID.randomUUID(); String original="uploads/it/original.txt", thumb="derivatives/it/thumb.jpg"; byte[] body="delete-me".getBytes(), thumbBody="thumb".getBytes();
        put(original,body,"text/plain"); put(thumb,thumbBody,"image/jpeg");
        FileMetadata file=files.saveAndFlush(new FileMetadata("original.txt","original.txt",original,"text/plain",body.length,"a".repeat(64),FileStatus.DELETING,ScanStatus.CLEAN,ThumbnailStatus.COMPLETED)); UUID fileId=file.getId();
        derivatives.saveAndFlush(new FileDerivative(fileId,DerivativeType.THUMBNAIL,thumb,"image/jpeg",thumbBody.length,300,300));
        send(new FileDeletedEvent(eventId,fileId,Instant.now()));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(()->{assertThat(files.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileStatus.DELETED);assertThat(derivatives.findByFileId(fileId)).isEmpty();assertThat(objectStorage.exists(original)).isFalse();assertThat(objectStorage.exists(thumb)).isFalse();});
        assertThat(processed.countByEventIdAndConsumerName(eventId,WorkerNames.FILE_DELETION)).isEqualTo(1);
    }

    @Test void duplicateDeletionEventShouldBeProcessedOnlyOnce() throws Exception {
        UUID eventId=UUID.randomUUID(); String key="uploads/it/duplicate.txt"; byte[] body="duplicate".getBytes(); put(key,body,"text/plain");
        FileMetadata file=files.saveAndFlush(new FileMetadata("duplicate.txt","duplicate.txt",key,"text/plain",body.length,"b".repeat(64),FileStatus.DELETING,ScanStatus.CLEAN,ThumbnailStatus.NOT_REQUIRED)); UUID fileId=file.getId();
        FileDeletedEvent event=new FileDeletedEvent(eventId,fileId,Instant.now()); String payload=objectMapper.writeValueAsString(event); kafkaTemplate.send(TOPIC,fileId.toString(),payload).get(); kafkaTemplate.send(TOPIC,fileId.toString(),payload).get();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(()->{assertThat(files.findById(fileId).orElseThrow().getStatus()).isEqualTo(FileStatus.DELETED);assertThat(objectStorage.exists(key)).isFalse();});
        assertThat(processed.countByEventIdAndConsumerName(eventId,WorkerNames.FILE_DELETION)).isEqualTo(1);
    }
    private void put(String k,byte[] b,String ct)throws Exception{objectStorage.put(k,new ByteArrayInputStream(b),b.length,ct);} private void send(FileDeletedEvent e)throws Exception{kafkaTemplate.send(TOPIC,e.fileId().toString(),objectMapper.writeValueAsString(e)).get();}
}
