package com.example.scorer.service;

import com.example.scorer.api.GithubApiClient;
import com.example.scorer.config.GithubApiProperties;
import com.example.scorer.model.Pagination;
import com.example.scorer.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GithubSearchService {

    private final GithubApiClient githubApiClient;
    private final ScoringService scoringService;
    private final GithubApiProperties githubApiProperties;

    public GithubSearchService(
            GithubApiClient githubApiClient,
            ScoringService scoringService,
            GithubApiProperties githubApiProperties
    ) {
        this.githubApiClient = githubApiClient;
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

        var repositories = githubApiClient.searchRepositories(createdAfter, language, pagination);

        return repositories.parallelStream()
                .map(repo -> new ScoreResult(
                        repo.getName(),
                        repo.getStars(),
                        repo.getForks(),
                        repo.getUpdatedAt(),
                        scoringService.calculateScore(repo)))
                .collect(Collectors.toList());
    }
}
