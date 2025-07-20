package com.example.scorer.model;

import com.example.scorer.config.GithubApiProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pagination {
    private final int pageSize;
    private final int maxPages;

    public static Pagination fromProperties(GithubApiProperties props) {
        return new Pagination(props.getPageSize(), props.getMaxPages());
    }

    public static Pagination withOverrides(GithubApiProperties props, Integer pageSize, Integer maxPages) {
        return new Pagination(
                pageSize != null ? Math.min(pageSize, props.getPageSize()) : props.getPageSize(),
                maxPages != null ? Math.min(maxPages, props.getMaxPages()) : props.getMaxPages()
        );
    }
}

