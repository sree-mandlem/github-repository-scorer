package com.example.scorer.service;

import com.example.scorer.model.RepositoryDto;

public interface ScoringStrategy {
    double calculateScore(RepositoryDto repo);
}
