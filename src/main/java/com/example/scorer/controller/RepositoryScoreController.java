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
            @RequestParam String created_after,
            @RequestParam String language,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer maxPages) {

        List<ScoreResult> results = (pageSize == null && maxPages == null)
                ? githubSearchService.fetchAndScoreRepositories(created_after, language)
                : githubSearchService.fetchAndScoreRepositories(created_after, language, pageSize, maxPages);

        return ResponseEntity.ok(results);
    }
}