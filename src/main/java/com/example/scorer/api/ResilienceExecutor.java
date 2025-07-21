package com.example.scorer.api;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Slf4j
public class ResilienceExecutor {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ResilienceExecutor(
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry) {
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public <T> T execute(String name, Supplier<T> supplier, Function<Throwable, T> fallback) {
        var decorated = decorate(name, supplier);

        try {
            log.debug("Executing operation '{}' with decorators (Retry, CircuitBreaker, RateLimiter)", name);

            T result = decorated.get();
            log.debug("Operation '{}' succeeded", name);
            return result;
        } catch (Exception e) {
            log.warn("Fallback triggered for '{}': {}", name, e.getMessage());
            return fallback.apply(e);
        }
    }

    private <T> Supplier<T> decorate(String name, Supplier<T> supplier) {
        return RateLimiter.decorateSupplier(rateLimiterRegistry.rateLimiter(name),
                CircuitBreaker.decorateSupplier(circuitBreakerRegistry.circuitBreaker(name),
                        Retry.decorateSupplier(retryRegistry.retry(name), supplier)
                )
        );
    }
}
