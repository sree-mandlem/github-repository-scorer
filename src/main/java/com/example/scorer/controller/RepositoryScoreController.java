package com.example.scorer.controller;

import com.example.scorer.model.ScoreResult;
import com.example.scorer.service.GithubSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scorer/api/repositories")
@Slf4j
public class RepositoryScoreController {

    private final GithubSearchService githubSearchService;

    public RepositoryScoreController(GithubSearchService githubSearchService) {
        this.githubSearchService = githubSearchService;
    }

    @GetMapping("/score")
    public ResponseEntity<List<ScoreResult>> getScoredRepositories(
            @RequestParam("created_after") String createdAfter,
            @RequestParam("language") String language) {

        log.info("Received request to score repositories created after '{}' with language '{}'",
                createdAfter, language);
        return ResponseEntity.ok(githubSearchService.fetchAndScoreRepositories(createdAfter, language));
    }
}