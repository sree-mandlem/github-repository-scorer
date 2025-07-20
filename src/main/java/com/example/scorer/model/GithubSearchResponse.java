package com.example.scorer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GithubSearchResponse {

    @JsonProperty("items")
    private List<RepositoryDto> items;
}