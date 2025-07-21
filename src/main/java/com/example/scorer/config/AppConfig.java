package com.example.scorer.config;

import com.example.scorer.api.GithubRateLimitInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(GithubApiProperties.class)
public class AppConfig {

    @Bean(name = "githubExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // adjust as needed
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("GitHubPage-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .additionalInterceptors(new GithubRateLimitInterceptor()) // Log Rate limits/Usage by Github Search api
                .build();
    }
}