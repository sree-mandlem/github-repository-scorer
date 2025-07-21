package com.example.scorer.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class GithubRateLimitInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {
        var response = execution.execute(request, body);
        logRateLimitHeaders(response.getHeaders());
        return response;
    }

    private void logRateLimitHeaders(HttpHeaders headers) {
        var limit = headers.getFirst("X-RateLimit-Limit");
        var remaining = headers.getFirst("X-RateLimit-Remaining");
        var reset = headers.getFirst("X-RateLimit-Reset");

        if (limit != null && remaining != null && reset != null) {
            var resetTime = Instant.ofEpochSecond(Long.parseLong(reset));
            var secondsUntilReset = Duration.between(Instant.now(), resetTime).getSeconds();

            log.info("GitHub Rate Limit: {}/{} remaining. Resets in {} seconds at {}.",
                    remaining, limit, secondsUntilReset, resetTime);
        } else {
            log.info("GitHub rate limit headers not present.");
        }
    }
}

