package com.example.scorer.service;

import com.example.scorer.model.RepositoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A simple scoring strategy using repo stars, forks and recency factor as criteria with weighted calculation,
 * Score = stars × 1.0 + forks × 0.75 + max(0, 30 - days\_since\_update)
 */
@Slf4j
@Component
public class SimpleScoringStrategy implements ScoringStrategy {
    @Override
    public double calculateScore(RepositoryDto repo) {
        var daysSinceUpdate = ChronoUnit.DAYS.between(repo.getUpdatedAt(), Instant.now());
        var recencyFactor = Math.max(0, 30 - daysSinceUpdate);
        var score = repo.getStars() * 1.0 + repo.getForks() * 0.75 + recencyFactor;

        log.debug("Scoring '{}' -> stars: {}, forks: {}, recencyFactor: {}, score: {}",
                repo.getName(), repo.getStars(), repo.getForks(), recencyFactor, score);

        return score;
    }
}
