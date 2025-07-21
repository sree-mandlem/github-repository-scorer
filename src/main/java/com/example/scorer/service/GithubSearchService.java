package com.example.scorer.service;

import com.example.scorer.config.GithubApiProperties;
import com.example.scorer.model.Pagination;
import com.example.scorer.model.RepositoryDto;
import com.example.scorer.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
@Slf4j
public class GithubSearchService {

    private final AsyncGithubRepositoryFetcher asyncFetcher;
    private final ScoringService scoringService;
    private final GithubApiProperties githubApiProperties;

    public GithubSearchService(
            AsyncGithubRepositoryFetcher asyncFetcher,
            ScoringService scoringService,
            GithubApiProperties githubApiProperties
    ) {
        this.asyncFetcher = asyncFetcher;
        this.scoringService = scoringService;
        this.githubApiProperties = githubApiProperties;
    }

    public List<ScoreResult> fetchAndScoreRepositories(String createdAfter, String language) {
        Pagination pagination = Pagination.fromProperties(githubApiProperties);
        return getScoreResults(createdAfter, language, pagination);
    }

    public List<ScoreResult> fetchAndScoreRepositories(
            String createdAfter, String language, Integer pageSize, Integer maxPages) {
        Pagination pagination = Pagination.withOverrides(githubApiProperties, pageSize, maxPages); // To prevent abuse
        return getScoreResults(createdAfter, language, pagination);
    }

    private List<ScoreResult> getScoreResults(String createdAfter, String language, Pagination pagination) {
        log.info("Fetching and scoring repositories created after '{}' with language '{}', page size '{}' and max pages '{}'", createdAfter, language, pagination.getPageSize(), pagination.getMaxPages());

        List<CompletableFuture<List<RepositoryDto>>> futures = IntStream.rangeClosed(1, pagination.getMaxPages())
                .mapToObj(page -> asyncFetcher.fetchPage(createdAfter, language, page, pagination.getPageSize()))
                .toList();

        List<RepositoryDto> allRepos = futures.stream()
                .map(future -> {
                    try {
                        List<RepositoryDto> result = future.join();
                        return result != null ? result : List.<RepositoryDto>of();
                    } catch (Exception e) {
                        log.warn("Failed to fetch GitHub repositories asynchronously", e);
                        return List.<RepositoryDto>of();
                    }
                })
                .flatMap(List::stream)
                .toList();

        return allRepos.parallelStream()
                .map(repo -> new ScoreResult(
                        repo.getName(),
                        repo.getStars(),
                        repo.getForks(),
                        repo.getUpdatedAt(),
                        scoringService.calculateScore(repo)))
                .toList();
    }
}
