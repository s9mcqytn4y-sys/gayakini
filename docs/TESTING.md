# Testing Strategy

## Overview
We prioritize a stable, high-coverage, and machine-readable test suite. The build fails-fast if any quality gate is not met.

## Testing Pyramid

1.  **Unit Tests**: Standard JUnit 5 and MockK. Located in `src/test/kotlin/**/domain/` or `src/test/kotlin/**/application/`.
    -   *Constraint*: No Spring context, no Database, no Docker.
2.  **Web Slice Tests**: `@WebMvcTest` for controller, security, and JSON mapping verification.
    -   *Standard*: Use `MockMvc` and verify RBAC rules in isolation.
3.  **Integration Tests**: Full-context tests using **Testcontainers (PostgreSQL 17)**.
    -   *Infrastructure*: Use Spring Boot 3.4 `@ServiceConnection` for automatic container configuration.
    -   *Constraint*: **Never use H2.** All persistence tests must run against a real PostgreSQL container.
4.  **External API Tests**: Stubbed via **WireMock**.
    -   *Standard*: Mock external providers (e.g., Midtrans, Biteship) to ensure deterministic results.

## Quality Gates & Coverage

### JetBrains Kover
We enforce instruction coverage across the application.
-   **Current Baseline**: **42%** (Hardened release baseline).
-   **Target**: **80%**.
-   **Verification**: The build will fail if coverage falls below the current baseline.
-   **Exclusions**: DTOs, application entry points (`*ApplicationKt`), and infrastructure configurations are excluded from the metric.
-   **Local Report**: Run `./gradlew koverHtmlReport` to generate a report.
-   **Output**: `build/reports/kover/html/index.html`.

### Test Logging
Logs are optimized for both humans and CI agents:
-   **Success**: Logs are suppressed to reduce noise.
-   **Failure**: Detailed failure information is shown using `SHORT` stack trace formatting for clarity.
-   **Summary**: A clean summary block is printed at the end of the test suite execution.

## Execution
-   **The Canonical Gate**: `./gradlew ciBuild` (Runs Lint -> Detekt -> Test -> Kover -> BootJar).
-   **Standard Test**: `./gradlew test`.
-   **Fast Mode**: `./gradlew test -PexcludeIntegration` (Skips Docker-dependent integration tests).

## Integration Test Example (`@ServiceConnection`)

```kotlin
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }

    @Test
    fun `should persist data`() {
        // ...
    }
}
```

## Local Database Fallback (Windows/CI Compatibility)
While Testcontainers is the preferred method, we support a fallback to a local PostgreSQL instance (e.g., via Docker Compose) for environments where Testcontainers might be unstable (like some Windows configurations) or to speed up local test cycles.

### How to use Local Fallback:
1. Ensure the local database is running: `docker compose up -d gayakini-db`.
2. Run tests with the property disabled:
   ```bash
   ./gradlew test -Dtestcontainers.enabled=false
   ```
   Or via environment variable:
   ```bash
   TESTCONTAINERS_ENABLED=false ./gradlew test
   ```

When disabled, the `BaseDbIntegrationTest` will automatically attempt to connect to `localhost:5432` using the default credentials defined in `docker-compose.yml`.
