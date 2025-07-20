package com.example.scorer.api;

import com.example.scorer.model.GithubSearchResponse;
import com.example.scorer.model.RepositoryDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class GithubApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.api.base-url:https://api.github.com}")
    private String githubBaseUrl;

    public GithubApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<RepositoryDto> searchRepositories(String createdAfter, String language) {
        log.info("Initiating GitHub repository search for language='{}', createdAfter='{}'", language, createdAfter);

        var url = buildSearchUrl(createdAfter, language);
        log.debug("GitHub API URL: {}", url);

        try {
            var response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            log.info("GitHub API responded with status: {}", response.getStatusCode());

            var searchResponse = objectMapper.readValue(response.getBody(), GithubSearchResponse.class);
            log.debug("Parsed {} repositories from GitHub response", searchResponse.getItems().size());

            return searchResponse.getItems();

        } catch (HttpServerErrorException e) {
            log.error("GitHub API request failed: {}", e.getMessage());
            throw e; // Retryable
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitHub response", e);
            throw new RuntimeException("JSON processing error", e);
        } catch (RestClientException e) {
            log.warn("GitHub API request failed: {}", e.getMessage());
            throw e; // Retryable
        }
    }

    private String buildSearchUrl(String createdAfter, String language) {
        return String.format(
                "%s/search/repositories?q=language:%s+created:>%s&sort=stars&order=desc&per_page=100",
                githubBaseUrl, language, createdAfter
        );
    }
}
