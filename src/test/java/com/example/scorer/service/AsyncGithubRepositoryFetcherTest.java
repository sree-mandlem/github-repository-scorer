package com.example.scorer.service;

import com.example.scorer.api.GithubApiClient;
import com.example.scorer.model.RepositoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncGithubRepositoryFetcherTest {

    @Mock
    private GithubApiClient githubApiClient;

    private AsyncGithubRepositoryFetcher fetcher;

    @BeforeEach
    void setup() {
        fetcher = new AsyncGithubRepositoryFetcher(githubApiClient);
    }

    @Test
    void shouldReturnRepositories_whenApiClientSucceeds() throws Exception {
        String createdAfter = "2024-01-01";
        String language = "Java";
        int page = 1;
        int pageSize = 100;
        RepositoryDto dto = new RepositoryDto("test-repo", 100, 50, Instant.now());
        List<RepositoryDto> expectedList = List.of(dto);
        when(githubApiClient.searchRepositories(createdAfter, language, page, pageSize))
                .thenReturn(expectedList);

        CompletableFuture<List<RepositoryDto>> future =
                fetcher.fetchPage(createdAfter, language, page, pageSize);

        assertThat(future).isCompleted();
        assertThat(future.get()).containsExactly(dto);
        verify(githubApiClient, times(1)).searchRepositories(createdAfter, language, page, pageSize);
    }

    @Test
    void shouldReturnFailedFuture_whenApiClientThrowsException() {
        String createdAfter = "2024-01-01";
        String language = "Java";
        int page = 2;
        int pageSize = 50;
        RuntimeException exception = new RuntimeException("GitHub API failed");
        when(githubApiClient.searchRepositories(createdAfter, language, page, pageSize))
                .thenThrow(exception);

        CompletableFuture<List<RepositoryDto>> future =
                fetcher.fetchPage(createdAfter, language, page, pageSize);

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("GitHub API failed");
    }
}
