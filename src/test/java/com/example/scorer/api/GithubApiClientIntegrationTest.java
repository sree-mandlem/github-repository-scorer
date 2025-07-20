package com.example.scorer.api;

import com.example.scorer.model.Pagination;
import com.example.scorer.model.RepositoryDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ActiveProfiles("test")
class GithubApiClientIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GithubApiClient githubApiClient;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldReturnParsedRepositories_onValidResponse() {
        String mockJson = """
        {
          "items": [
            {
              "name": "awesome-repo",
              "stargazers_count": 150,
              "forks_count": 25,
              "updated_at": "2024-06-01T12:00:00Z"
            }
          ]
        }
        """;

        server.expect(ExpectedCount.once(), requestTo(containsString("/search/repositories")))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        Pagination pagination = new Pagination(100, 1);
        List<RepositoryDto> results = githubApiClient.searchRepositories("2024-01-01", "Java", pagination);

        assertThat(results)
                .extracting(RepositoryDto::getName, RepositoryDto::getStars, RepositoryDto::getForks)
                .containsExactly(tuple("awesome-repo", 150, 25));

        server.verify();
    }

    @Test
    void shouldSupportMultiplePages() {
        String page1Json = """
        {
          "items": [
            {
              "name": "repo-1",
              "stargazers_count": 100,
              "forks_count": 10,
              "updated_at": "2024-05-01T12:00:00Z"
            }
          ]
        }
        """;

        String page2Json = """
        {
          "items": [
            {
              "name": "repo-2",
              "stargazers_count": 200,
              "forks_count": 20,
              "updated_at": "2024-06-01T12:00:00Z"
            }
          ]
        }
        """;

        String emptyPageJson = """
        {
          "items": []
        }
        """;

        server.expect(ExpectedCount.once(), requestTo(containsString("page=1")))
                .andRespond(withSuccess(page1Json, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(containsString("page=2")))
                .andRespond(withSuccess(page2Json, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(containsString("page=3")))
                .andRespond(withSuccess(emptyPageJson, MediaType.APPLICATION_JSON));

        Pagination pagination = new Pagination(100, 3);
        List<RepositoryDto> results = githubApiClient.searchRepositories("2024-01-01", "Java", pagination);

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(RepositoryDto::getName)
                .containsExactlyInAnyOrder("repo-1", "repo-2");

        server.verify();
    }

    @Test
    void shouldThrowOnHttp5xx() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        Pagination pagination = new Pagination(100, 1);
        assertThrows(HttpServerErrorException.class,
                () -> githubApiClient.searchRepositories("2024-01-01", "Java", pagination));
    }

    @Test
    void shouldThrowOnInvalidJson() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withSuccess("INVALID_JSON", MediaType.APPLICATION_JSON));

        Pagination pagination = new Pagination(100, 1);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> githubApiClient.searchRepositories("2024-01-01", "Java", pagination));
        assertThat(ex.getMessage()).contains("JSON processing error");
    }

    @Test
    void shouldThrowOnRestClientException() {
        server.expect(requestTo(containsString("/search/repositories")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        Pagination pagination = new Pagination(100, 1);
        assertThrows(RestClientException.class,
                () -> githubApiClient.searchRepositories("2024-01-01", "Java", pagination));
    }

    @Test
    void shouldStopFetchingIfFirstPageIsEmpty() {
        String emptyPageJson = """
            {
              "items": []
            }
            """;

        server.expect(ExpectedCount.once(), requestTo(containsString("page=1")))
                .andRespond(withSuccess(emptyPageJson, MediaType.APPLICATION_JSON));

        Pagination pagination = new Pagination(100, 5);
        List<RepositoryDto> results = githubApiClient.searchRepositories("2024-01-01", "Java", pagination);

        assertThat(results).isEmpty();
        server.verify();
    }
}
