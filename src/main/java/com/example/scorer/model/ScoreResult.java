package com.example.scorer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResult {
    private String name;
    private int stars;
    private int forks;
    private Instant lastUpdated;
    private double score;
}
