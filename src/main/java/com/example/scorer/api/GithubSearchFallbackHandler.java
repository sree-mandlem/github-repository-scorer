package com.example.scorer.api;

import com.example.scorer.model.RepositoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * This is a fallback handler in case a Github search API fails while rate limiting, network/connectivity issues.
 * This can be further extended to serve a cached result, in memory using libraries like *Caffeine* or *Redis* for scale.
 */
@Component
@Slf4j
public class GithubSearchFallbackHandler {
    public List<RepositoryDto> handle(String createdAfter, String language, int page, int pageSize, Throwable t) {
        log.warn("Fallback triggered for GitHub search - language='{}', createdAfter='{}', page='{}', page size '{}'",
                language, createdAfter, page, pageSize, t);
        return Collections.emptyList(); // Or cached result from Caffeine or Redis
    }
}