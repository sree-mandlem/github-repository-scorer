# GitHub Repository Scorer

```markdown
A Spring Boot application that fetches, scores, and ranks GitHub repositories based on **stars**, **forks**, and **update recency** using a configurable scoring strategy.

## 🚀 Features

- Fetches repositories from GitHub's public [Search API](https://docs.github.com/en/rest/search/search?apiVersion=2022-11-28#search-repositories)
- Applies a scoring formula to rank repositories
- Resilience with **Resilience4j**: Retry, Circuit Breaker, and Rate Limiter
- Async processing with `@Async` and `CompletableFuture`
- Metrics and logging for retries, failures, fallbacks
- Clean modular architecture for testing and extension

## ⚙️ Scoring Formula

By default:
```

Score = stars × 1.0 + forks × 0.75 + max(0, 30 - days\_since\_update)

````

## 📦 Tech Stack

- Java 17
- Spring Boot 3.5.x
- Resilience4j
- Jackson (with Java Time Module)
- JUnit 5 + Mockito + AssertJ
- Awaitility + MockRestServiceServer (for integration testing)
- Spring Doc Open API generator with Swagger UI (⚠️ Has known vulnerabilities)

## 📥 API Usage

### `GET /scorer/api/repositories/score`

| Param          | Required | Example         |
|----------------|----------|-----------------|
| `created_after`| ✅        | `2024-01-01    |
| `language`     | ✅        | `Java`         |
| `pageSize`     |           | `100`          |
| `maxPages`     |           | `10`           |

**Response:**
```json
[
  {
    "name": "awesome-repo",
    "stars": 150,
    "forks": 25,
    "updatedAt": "2024-07-01T10:00:00Z",
    "score": 175.0
  }
]
````

## 🧪 Running Tests

```bash
./mvnw test
```

Includes:

* Tests for services
* Integration tests for GitHub API client with fallback + retry metrics
* Logging and circuit breaker state assertions

## 🔐 Rate Limit Awareness

Since the application relies on Github Public Search API, the application respects the Rate Limiting settings by Github and react accordingly. Resilience4j is used to achieve this and also provides additional functionalities like Circuit Breaker and Retry with fallback mechanics.

* Logs GitHub rate limit headers (`X-RateLimit-Remaining`, `X-RateLimit-Reset`)
* Automatically delays or falls back based on Resilience4j policies

## 📁 Project Structure

```
src/
├── api/                 # Controller layer
├── client/              # GitHub API integration
├── config/              # Async + Resilience config
├── model/               # DTOs and responses
├── service/             # Scoring and orchestration logic
└── fallback/            # Central fallback handling
```

## 🛠️ Setup

### Prerequisites

* Java 17
* Maven 3+

### Run the application from IDE/CMD

```bash
./mvnw spring-boot:run
```

### Run application using Docker

```bash
./mvnw clean package -DskipTests
```
Build Docker image
```bash
docker build -t github-repository-scorer .
```
Run container on 8080(Make sure that no other application is using this port)
```bash
docker run -p 8080:8080 github-repository-scorer
```

Then visit http://localhost:8080/scorer/swagger-ui.html for API Specification

Using Postman: [http://localhost:8080/scorer/api/repositories/score](http://localhost:8080/scorer/api/repositories/score) with API Usgae as above.


## 📜 Configuration (application.yml)

The current properties and values are ideal for dev/test cases, but these values need to be optimized for prod, to address different scaling, latency and performance issues.

```yaml
resilience4j:
  retry:
    instances:
      githubSearch:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - (...)
  circuitbreaker:
    instances:
      githubSearch:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 15s
  ratelimiter:
    instances:
      githubSearch:
        limit-for-period: 10
        limit-refresh-period: 1s
        timeout-duration: 0
```

## ✅ Further plans

* Move Async strategy to a Kafka based solution
* Add caching (e.g., Caffeine or Redis)
* Move towards performant pre-fetch solution backed by DB and Cache
* Expose scoring config via endpoint
* Support advanced pagination with cursors from GitHub
* Add Prometheus/Grafana monitoring
