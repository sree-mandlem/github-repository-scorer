package com.example.scorer.service;

import com.example.scorer.api.GithubApiClient;
import com.example.scorer.model.RepositoryDto;
import com.example.scorer.model.ScoreResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private GithubSearchService searchService;

    @Test
    void fetchAndScoreRepositories_shouldReturnScoredResults() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        RepositoryDto repo = new RepositoryDto("test-repo", 100, 50, Instant.now());
        double score = 175.0;
        Mockito.when(scoringService.calculateScore(repo)).thenReturn(score);
        when(githubApiClient.searchRepositories(createdAfter, language)).thenReturn(List.of(repo));

        List<ScoreResult> result = searchService.fetchAndScoreRepositories(createdAfter, language);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("test-repo");
        assertThat(result.get(0).getScore()).isEqualTo(score);
        verify(scoringService, times(1)).calculateScore(repo);
        verify(githubApiClient).searchRepositories(createdAfter, language);
    }

    @Test
    void fetchAndScoreRepositories_shouldThrowHttpServerErrorException() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        when(githubApiClient.searchRepositories(eq(createdAfter), eq(language)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> searchService.fetchAndScoreRepositories(createdAfter, language))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessage("500 INTERNAL_SERVER_ERROR");    }

    @Test
    void fetchAndScoreRepositories_shouldThrowJsonProcessingException() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        when(githubApiClient.searchRepositories(eq(createdAfter), eq(language)))
                .thenThrow(new RuntimeException("Failed to parse") {});

        assertThatThrownBy(() -> searchService.fetchAndScoreRepositories(createdAfter, language))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to parse");
    }

    @Test
    void fetchAndScoreRepositories_shouldThrowRestClientException() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        when(githubApiClient.searchRepositories(eq(createdAfter), eq(language)))
                .thenThrow(new RestClientException("Connection failed"));

        assertThatThrownBy(() -> searchService.fetchAndScoreRepositories(createdAfter, language))
                .isInstanceOf(RestClientException.class)
                .hasMessage("Connection failed");
    }
}

