package com.example.scorer.controller;

import com.example.scorer.model.ScoreResult;
import com.example.scorer.service.GithubSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepositoryScoreController.class)
public class RepositoryScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GithubSearchService githubSearchService;

    @Test
    public void testGetScoredRepositories() throws Exception {
        Instant lastUpdated = Instant.now();
        ScoreResult mockResult = new ScoreResult("repo", 100, 50, lastUpdated, 175);
        Mockito.when(githubSearchService.fetchAndScoreRepositories("2022-01-01", "Java"))
                .thenReturn(List.of(mockResult));

        mockMvc.perform(get("/scorer/api/repositories/score")
                        .param("created_after", "2022-01-01")
                        .param("language", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("repo"))
                .andExpect(jsonPath("$[0].stars").value(100))
                .andExpect(jsonPath("$[0].forks").value(50))
                .andExpect(jsonPath("$[0].lastUpdated").value(lastUpdated.toString()))
                .andExpect(jsonPath("$[0].score").value(175));

    }

    @Test
    public void testGetScoredRepositories_failure() throws Exception {
        Mockito.when(githubSearchService.fetchAndScoreRepositories("2022-01-01", "Java"))
                .thenThrow(new RuntimeException("GitHub API error"));

        mockMvc.perform(get("/scorer/api/repositories/score")
                        .param("created_after", "2022-01-01")
                        .param("language", "Java"))
                .andExpect(status().isInternalServerError());
    }
}
