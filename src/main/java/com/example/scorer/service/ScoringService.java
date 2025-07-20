package com.example.scorer.service;

import com.example.scorer.model.RepositoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * This scoring orchestrator simplifies strategy that is used for scoring.
 * For now the strategy is a simple scoring algorithm, but this should manage any future complicated scoring algorithms
 * and keep the scoring logic decoupled from actual service.
 *
 */
@Service
@Slf4j
public class ScoringService {

    private final ScoringStrategy scoringStrategy;

    public ScoringService(@Qualifier("simpleScoringStrategy") ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }

    public double calculateScore(RepositoryDto repo) {
        double score = scoringStrategy.calculateScore(repo);
        log.debug("Calculated score for repository '{}': {}", repo.getName(), score);
        return score;
    }
}