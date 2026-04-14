## Testing Strategy

## Overview
We prioritize a stable and reliable test suite using JUnit 5. All tests are executed via the Gradle `test` task. The testing strategy follows a modern pyramid:

1. **Pure Unit Tests**: High-speed, deterministic verification of domain logic and utilities. **No Spring context, no DB, no HTTP, no Docker.**
2. **Web Slice Tests**: `@WebMvcTest` to verify controllers, security, and JSON mapping in isolation.
3. **Integration Tests**: Full-context tests using **Testcontainers (PostgreSQL)** to ensure parity with production.

## Test Tiers

### 1. Pure Unit Tests
Located in `src/test/kotlin/**/domain/` or `src/test/kotlin/**/util/`.
- **Scope**: Entities, value objects, utility classes, and service logic (mocked).
- **Execution**: Extremely fast (<1s per class).
- **Frameworks**: JUnit 5, MockK.
- **Constraints**: 
    - No `@SpringBootTest`.
    - No `@ExtendWith(SpringExtension.class)`.
    - No database or external service calls.
- **Example**: `OrderTest.kt`, `HashUtilsTest.kt`.

### 2. Web Slice Tests
Located in `src/test/kotlin/**/api/`.
- **Scope**: API endpoints, security annotations (RBAC), request/response validation, and JSON mapping.
- **Execution**: Moderate speed (boots a minimal Spring context with `@WebMvcTest`).
- **Standard**: All web slice tests should extend `BaseWebMvcTest` and use `MockMvc`.
- **Mocking**: Mocks are used for all underlying services to keep tests focused on the web layer and avoid requiring a database.
- **Security**: Security behavior is verified using mock JWT tokens (e.g., `valid-admin-token`, `valid-customer-token`) configured in `BaseWebMvcTest.SecurityTestConfig`.
- **Example**: `ProductControllerTest.kt`, `AdminProductControllerTest.kt`.

### 3. Integration Tests
Located in `src/test/kotlin/**/integration/`.
- **Scope**: Persistence, transaction boundaries, end-to-end flows.
- **Execution**: Slower (requires Docker).
- **Frameworks**: `@SpringBootTest`, Testcontainers.
- **Example**: `OrderStateIntegrationTest.kt`.

## Running Tests
- **All Tests**: `./gradlew test` (Requires Docker)
- **Fast Tests (No Docker)**: `./gradlew test -PexcludeIntegration` (Skips `@Tag("integration")` tests)
- **CI Pass**: `./gradlew ciBuild` (Runs all checks, linting, and coverage)
- **Local CI Pipeline**: `./scripts/ci.sh` (Mimics GitHub Actions locally)
- **Coverage**: `./gradlew koverHtmlReport` (Reports in `build/reports/kover/html`)

## Key Technologies
- **Testcontainers**: Provides a real PostgreSQL 16 instance for integration tests via `@Tag("integration")`. **Requirement**: Local Docker daemon must be running for these tests to pass.
- **MockK**: Idiomatic Kotlin mocking library.
- **WireMock**: Mocks external HTTP services (Midtrans, Biteship) for isolated provider testing. External providers like `MidtransPaymentProvider` and `BiteshipShippingProvider` use `RestTemplate` to allow transparent redirection to WireMock during tests.
- **ServiceConnection**: Automatically configures Spring Boot to use containerized services.

## Best Practices
- **Database Parity**: Never use H2 for integration tests. Always extend `BaseIntegrationTest`.
- **Isolation**: Use `@WebMvcTest` for API verification to avoid starting the full database context where not needed.
- **Mocks**: Prefer `relaxed = true` for MockK only when the specific behavior isn't the focus of the test.
- **Environment Isolation**: The CI environment may not have Docker available. Ensure unit and web-layer tests are prioritized and verified locally before integration tests.
