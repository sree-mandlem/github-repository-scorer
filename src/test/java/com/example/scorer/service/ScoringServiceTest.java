package com.example.scorer.service;

import com.example.scorer.model.RepositoryDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest
public class ScoringServiceTest {

    @Autowired
    private ScoringService scoringService;

    @Test
    public void testScoreCalculation() {
        RepositoryDto repo = new RepositoryDto("repo", 100, 50, Instant.now().minus(5, ChronoUnit.DAYS));

        double score = scoringService.calculateScore(repo);
        Assertions.assertTrue(score > 100);
    }
}
