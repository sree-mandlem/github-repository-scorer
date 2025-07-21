package com.example.scorer.controller;

import com.example.scorer.model.ErrorResponse;
import com.example.scorer.model.ScoreResult;
import com.example.scorer.service.GithubSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "GitHub Scoring", description = "Endpoints for scoring GitHub repositories")
public class RepositoryScoreController {

    private final GithubSearchService githubSearchService;

    public RepositoryScoreController(GithubSearchService githubSearchService) {
        this.githubSearchService = githubSearchService;
    }

    @Operation(
            summary = "Score GitHub repositories",
            description = """
            Returns scored repositories created after a given date for the specified language.
            Optionally accepts pagination parameters (`pageSize`, `maxPages`).
            ❌ Note: API may fallback or degrade if GitHub is rate-limited.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully scored repositories"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal error during scoring or GitHub API failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
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