package com.example.scorer.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class GithubRateLimiterInterceptorIntegrationTest {

    private final GithubRateLimitInterceptor interceptor = new GithubRateLimitInterceptor();

    @Test
    @SuppressWarnings("resource")
    void shouldLogRateLimitHeaders(CapturedOutput output) throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", "60");
        headers.add("X-RateLimit-Remaining", "10");
        headers.add("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(30).getEpochSecond()));
        ClientHttpResponse response = new MockClientHttpResponse("OK".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        response.getHeaders().putAll(headers);
        when(execution.execute(any(), any())).thenReturn(response);

        interceptor.intercept(mockRequest, new byte[0], execution);

        assertThat(output).contains("GitHub Rate Limit: 10/60 remaining");
    }

    @Test
    @SuppressWarnings("resource")
    void shouldLogDebugWhenHeadersAreMissing(CapturedOutput output) throws IOException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = new MockClientHttpResponse("OK".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        // No rate limit headers
        when(execution.execute(any(), any())).thenReturn(response);

        interceptor.intercept(mockRequest, new byte[0], execution);

        assertThat(output).contains("GitHub rate limit headers not present.");
    }
}

