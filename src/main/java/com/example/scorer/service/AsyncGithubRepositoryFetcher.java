package com.example.scorer.service;

import com.example.scorer.api.GithubApiClient;
import com.example.scorer.model.RepositoryDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncGithubRepositoryFetcher {

    private final GithubApiClient githubApiClient;

    public AsyncGithubRepositoryFetcher(GithubApiClient githubApiClient) {
        this.githubApiClient = githubApiClient;
    }

    @Async("githubExecutor")
    public CompletableFuture<List<RepositoryDto>> fetchPage(String createdAfter, String language, int page, int pageSize) {
        try {
            List<RepositoryDto> repos = githubApiClient.searchRepositories(createdAfter, language, page, pageSize);
            return CompletableFuture.completedFuture(repos);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
