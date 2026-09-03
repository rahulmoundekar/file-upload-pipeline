package com.rahul.config;

import com.rahul.exception.FileNotFoundForEventException;
import com.rahul.exception.InvalidEventException;
import com.rahul.exception.ObjectKeyMismatchException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerErrorHandlerConfig {

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> kafkaTemplate) {

        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    }


    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);

        backOff.setMaxElapsedTime(7000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.addNotRetryableExceptions(
                InvalidEventException.class,
                FileNotFoundForEventException.class,
                ObjectKeyMismatchException.class
        );

        return handler;
    }
}