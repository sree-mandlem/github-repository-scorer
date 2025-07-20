package com.example.scorer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.api")
@Data
public class GithubApiProperties {
    private int pageSize = 100;   // Default GitHub limit
    private int maxPages = 10;    // GitHub caps at 1000 results
}
