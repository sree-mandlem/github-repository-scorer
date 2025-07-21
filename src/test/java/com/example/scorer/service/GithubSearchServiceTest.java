package com.example.scorer.service;

import com.example.scorer.config.GithubApiProperties;
import com.example.scorer.model.RepositoryDto;
import com.example.scorer.model.ScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock
    private AsyncGithubRepositoryFetcher asyncFetcher;

    @Mock
    private ScoringService scoringService;

    @Mock
    private GithubApiProperties githubApiProperties;

    @InjectMocks
    private GithubSearchService searchService;

    @BeforeEach
    void setUp() {
        when(githubApiProperties.getPageSize()).thenReturn(10);
        when(githubApiProperties.getMaxPages()).thenReturn(100);
    }

    @Test
    void fetchAndScoreRepositories_shouldReturnScoredResults() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        RepositoryDto repo = new RepositoryDto("test-repo", 100, 50, Instant.now());
        double score = 175.0;
        Mockito.when(scoringService.calculateScore(repo)).thenReturn(score);
        when(asyncFetcher.fetchPage(eq(createdAfter), eq(language), eq(1), eq(10)))
                .thenReturn(CompletableFuture.completedFuture(List.of(repo)));

        List<ScoreResult> result = searchService.fetchAndScoreRepositories(createdAfter, language);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("test-repo");
        assertThat(result.get(0).getScore()).isEqualTo(score);
        verify(scoringService, times(1)).calculateScore(repo);
        verify(asyncFetcher).fetchPage(eq(createdAfter), eq(language), eq(1), eq(10));
    }

    @Test
    void fetchAndScoreRepositories_withPagination_shouldReturnScoredResults() {
        String createdAfter = "2022-01-01";
        String language = "Java";
        RepositoryDto repo1 = new RepositoryDto("test-repo1", 100, 50, Instant.now());
        RepositoryDto repo2 = new RepositoryDto("test-repo2", 100, 50, Instant.now());
        double score1 = 175.0;
        double score2 = 175.0;
        Mockito.when(scoringService.calculateScore(repo1)).thenReturn(score1);
        Mockito.when(scoringService.calculateScore(repo2)).thenReturn(score2);
        when(asyncFetcher.fetchPage(eq(createdAfter), eq(language), eq(1), eq(10)))
                .thenReturn(CompletableFuture.completedFuture(List.of(repo1)));
        when(asyncFetcher.fetchPage(eq(createdAfter), eq(language), eq(2), eq(10)))
                .thenReturn(CompletableFuture.completedFuture(List.of(repo2)));
        when(asyncFetcher.fetchPage(eq(createdAfter), eq(language), eq(3), eq(10)))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        List<ScoreResult> result = searchService.fetchAndScoreRepositories(createdAfter, language, 10, 100);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("test-repo1");
        assertThat(result.get(0).getScore()).isEqualTo(score1);
        verify(scoringService).calculateScore(repo2);
        verify(asyncFetcher).fetchPage(eq(createdAfter), eq(language), eq(1), eq(10));
        verify(asyncFetcher).fetchPage(eq(createdAfter), eq(language), eq(2), eq(10));
    }
}

