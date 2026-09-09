package com.rahul.ops;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PipelineMetrics {
    private final Counter uploads;
    private final Counter deletions;
    private final Counter workerFailures;
    private final Counter outboxFailures;
    private final Timer uploadTimer;

    public PipelineMetrics(MeterRegistry registry) {
        uploads = Counter.builder("files.uploaded.total").description("Total accepted uploads").register(registry);
        deletions = Counter.builder("files.deleted.total").description("Total completed deletions").register(registry);
        workerFailures = Counter.builder("workers.failures.total").description("Worker failures").register(registry);
        outboxFailures = Counter.builder("outbox.failures.total").description("Outbox publish failures").register(registry);
        uploadTimer = Timer.builder("files.upload.duration").description("Upload request duration").register(registry);
    }
    public Counter uploads(){return uploads;} public Counter deletions(){return deletions;} public Counter workerFailures(){return workerFailures;} public Counter outboxFailures(){return outboxFailures;} public Timer uploadTimer(){return uploadTimer;}
}
