package com.example.scorer.api;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ResilienceExecutorIntegrationTest {

    @Autowired private ResilienceExecutor resilienceExecutor;
    @Autowired private RetryRegistry retryRegistry;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired private RateLimiterRegistry rateLimiterRegistry;

    /**
     * Ensures a clean slate before each test by clearing or resetting
     * all registry-managed resilience components(to avoid carry-over).
     */
    @BeforeEach
    void setup() {
        retryRegistry.getAllRetries().forEach(r -> retryRegistry.remove(r.getName()));
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> rateLimiterRegistry.remove(rl.getName()));
    }

    @Test
    void successPath_shouldReturnResult_noFallback_noRetries() {
        String name = "successTest";
        retryRegistry.retry(name, RetryConfig.ofDefaults());
        circuitBreakerRegistry.circuitBreaker(name);
        rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.ofDefaults());

        String result = resilienceExecutor.execute(name, () -> "OK", t -> "FALLBACK");

        assertThat(result).isEqualTo("OK");
        assertThat(retryRegistry.retry(name))
                .extracting(Retry::getMetrics)
                .satisfies(m -> {
                    assertThat(m.getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
                    assertThat(m.getNumberOfSuccessfulCallsWithRetryAttempt()).isZero();
                });
        assertThat(circuitBreakerRegistry.circuitBreaker(name))
                .satisfies(circuitBreaker -> {
                    assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
                    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
                });
    }

    @Test
    void retryThenFallback_whenSupplierAlwaysFails() {
        String name = "retryFailTest";
        // Allows 1 retry attempt before triggering fallback
        retryRegistry.retry(name, RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .build());
        circuitBreakerRegistry.circuitBreaker(name);
        rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.ofDefaults());

        String result = resilienceExecutor.execute(
                name,
                () -> { throw new RuntimeException("Supplier failed"); },
                t -> "FALLBACK"
        );

        assertThat(result).isEqualTo("FALLBACK");
        assertThat(retryRegistry.retry(name).getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(circuitBreakerRegistry.circuitBreaker(name).getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void circuitBreaker_opensAfterRepeatedFailures() {
        String name = "cbOpenTest";
        retryRegistry.retry(name, RetryConfig.ofDefaults());
        circuitBreakerRegistry.circuitBreaker(name, CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build());
        rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.ofDefaults());

        // Trigger failures to open the circuit breaker
        for (int i = 0; i < 2; i++) {
            resilienceExecutor.execute(name, () -> { throw new RuntimeException("CB fail"); }, t -> "FALLBACK");
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        assertThat(cb.getState()).isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);

        // Should short-circuit and trigger fallback without executing the supplier
        String result = resilienceExecutor.execute(name, () -> "SHOULD_NOT_RUN", t -> "OPEN_FALLBACK");
        assertThat(result).isEqualTo("OPEN_FALLBACK");
    }

    @Test
    void rateLimiterDeniesSecondImmediateCall_triggersFallback() {
        String name = "rateLimitTest";
        retryRegistry.retry(name, RetryConfig.ofDefaults());
        circuitBreakerRegistry.circuitBreaker(name);
        rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .timeoutDuration(Duration.ZERO)
                .build());

        // First call succeeds
        assertThat(resilienceExecutor.execute(name, () -> "FIRST", t -> "FALLBACK1")).isEqualTo("FIRST");
        // Second call is rate-limited → fallback is triggered
        assertThat(resilienceExecutor.execute(name, () -> "SECOND", t -> "RATE_LIMIT_FALLBACK")).isEqualTo("RATE_LIMIT_FALLBACK");
    }

    @Test
    void retrySucceedsOnSecondAttempt_notUsingFallback() {
        String name = "retryRecoverTest";
        retryRegistry.retry(name, RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .build());
        circuitBreakerRegistry.circuitBreaker(name);
        rateLimiterRegistry.rateLimiter(name, RateLimiterConfig.ofDefaults());

        // Simulate transient failure: first call fails, second succeeds
        AtomicInteger counter = new AtomicInteger();
        Supplier<String> flaky = () -> {
            // AtomicInteger(not int) can safely change counter across retries even effectively final.
            if (counter.getAndIncrement() == 0) {
                throw new RuntimeException("transient");
            }
            return "SUCCESS_LATE";
        };

        assertThat(resilienceExecutor.execute(name, flaky, t -> "FALLBACK")).isEqualTo("SUCCESS_LATE");

        assertThat(retryRegistry.retry(name).getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }
}
