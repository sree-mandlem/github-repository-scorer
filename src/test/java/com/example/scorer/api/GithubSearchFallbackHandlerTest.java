package com.example.scorer.api;

import com.example.scorer.model.RepositoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GithubSearchFallbackHandlerTest {

    private GithubSearchFallbackHandler fallbackHandler;

    @BeforeEach
    void setUp() {
        fallbackHandler = new GithubSearchFallbackHandler();
    }

    @Test
    void handle_shouldReturnEmptyList_whenFallbackTriggered() {
        String createdAfter = "2024-01-01";
        String language = "Java";
        Throwable throwable = new RuntimeException("Simulated failure");

        List<RepositoryDto> result = fallbackHandler.handle(createdAfter, language, 1, 10, throwable);

        assertThat(result).isNotNull().isEmpty();
    }
}