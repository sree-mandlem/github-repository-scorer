package com.example.scorer.service;

import com.example.scorer.model.RepositoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleScoringStrategyTest {

    private SimpleScoringStrategy scoringStrategy;

    @BeforeEach
    public void setup() {
        scoringStrategy = new SimpleScoringStrategy();
    }

    @Test
    public void testScoreWithRecentUpdate() {
        RepositoryDto repo = new RepositoryDto("repo", 100, 40, Instant.now().minus(5, ChronoUnit.DAYS));

        double expectedRecency = 25.0; // 30 - 5
        double expectedScore = 100.0 + 40 * 0.75 + expectedRecency;

        double actualScore = scoringStrategy.calculateScore(repo);
        assertEquals(expectedScore, actualScore, 0.01);
    }

    @Test
    public void testScoreWithOldUpdateBeyond30Days() {
        RepositoryDto repo = new RepositoryDto("repo", 50, 20, Instant.now().minus(60, ChronoUnit.DAYS));

        double expectedScore = 50.0 + 20 * 0.75 + 0; // recencyFactor = 0

        double actualScore = scoringStrategy.calculateScore(repo);
        assertEquals(expectedScore, actualScore, 0.01);
    }

    @Test
    public void testScoreWithZeroStarsAndForks() {
        RepositoryDto repo = new RepositoryDto("repo", 0, 0, Instant.now());

        double expectedScore = 30.0; // full recencyFactor since updated today

        double actualScore = scoringStrategy.calculateScore(repo);
        assertEquals(expectedScore, actualScore, 0.01);
    }

    @Test
    public void testScoreWithNullUpdatedAt() {
        RepositoryDto repo = new RepositoryDto("repo", 10, 5, null);

        assertThrows(NullPointerException.class, () -> scoringStrategy.calculateScore(repo));
    }
}