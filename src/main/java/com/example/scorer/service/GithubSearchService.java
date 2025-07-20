package com.example.scorer.service;

import com.example.scorer.api.GithubApiClient;
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

    public GithubSearchService(GithubApiClient githubApiClient, ScoringService scoringService) {
        this.githubApiClient = githubApiClient;
        this.scoringService = scoringService;
    }

    public List<ScoreResult> fetchAndScoreRepositories(String createdAfter, String language) {
        log.info("Fetching and scoring repositories created after '{}' with language '{}'", createdAfter, language);

        var repositories = githubApiClient.searchRepositories(createdAfter, language);

        var results = repositories.parallelStream()
                .map(repo -> new ScoreResult(
                        repo.getName(),
                        repo.getStars(),
                        repo.getForks(),
                        repo.getUpdatedAt(),
                        scoringService.calculateScore(repo)))
                .collect(Collectors.toList());

        log.info("Scored {} repositories", results.size());
        return results;
    }
}
