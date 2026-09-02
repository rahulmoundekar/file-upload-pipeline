package com.rahul.service;

import com.rahul.config.OutboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryPolicyTest {

    private OutboxRetryPolicy policy;

    @BeforeEach
    void setUp() {

        policy = new OutboxRetryPolicy(new OutboxProperties(5, 1000, 60000));
    }

    @Test
    void shouldRetryBeforeMaximumAttempts() {

        assertThat(policy.shouldRetry(1)).isTrue();

        assertThat(policy.shouldRetry(4)).isTrue();
    }

    @Test
    void shouldNotRetryAfterMaximumAttempts() {

        assertThat(policy.shouldRetry(5)).isFalse();
    }

    @Test
    void shouldUseExponentialBackoff() {

        Instant before = Instant.now();

        Instant next = policy.nextAttemptAt(1);

        Duration delay = Duration.between(before, next);

        assertThat(delay.toMillis()).isBetween(900L, 1200L);
    }

    @Test
    void shouldCapBackoff() {

        Instant before = Instant.now();

        Instant next = policy.nextAttemptAt(10);

        Duration delay = Duration.between(before, next);

        assertThat(delay.toMillis()).isLessThanOrEqualTo(60000L);
    }
}