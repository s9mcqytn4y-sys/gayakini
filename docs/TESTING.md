# Testing Strategy

## Overview
We prioritize a stable, high-coverage, and machine-readable test suite. The build fails-fast if any quality gate is not met.

## Testing Pyramid

1.  **Unit Tests**: Standard JUnit 5 and MockK. Located in `src/test/kotlin/**/domain/` or `src/test/kotlin/**/application/`.
    -   *Constraint*: No Spring context, no Database, no Docker.
2.  **Web Slice Tests**: `@WebMvcTest` for controller, security, and JSON mapping verification.
    -   *Standard*: Use `MockMvc` and verify RBAC rules in isolation.
3.  **Integration Tests**: Full-context tests using **Testcontainers (PostgreSQL 16)**.
    -   *Infrastructure*: Use Spring Boot 3.4 `@ServiceConnection` for automatic container configuration.
    -   *Constraint*: **Never use H2.** All persistence tests must run against a real PostgreSQL container.
4.  **External API Tests**: Stubbed via **WireMock**.
    -   *Standard*: Mock external providers (e.g., Midtrans, Biteship) to ensure deterministic results.

## Quality Gates & Coverage

### JetBrains Kover
We enforce a minimum of **80% instruction coverage** across the application.
-   **Verification**: The build will fail if coverage falls below 80%.
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
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Test
    fun `should persist data`() {
        // ...
    }
}
```
