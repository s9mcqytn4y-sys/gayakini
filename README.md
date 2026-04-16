# Gayakini Backend

[![CI](https://github.com/gayakini/gayakini/actions/workflows/ci.yml/badge.svg)](https://github.com/gayakini/gayakini/actions/workflows/ci.yml)

Backend architecture for Gayakini, an e-commerce platform for industrial suppliers.

## Tech Stack
- **JDK 17**
- **Kotlin 2.0.21**
- **Spring Boot 3.4.0**
- **PostgreSQL** (Flyway for migrations)

## Getting Started

### Prerequisites
- JDK 17
- Docker (for PostgreSQL and Mailpit)

### Full Stack (App + Infrastructure)
```bash
docker compose up -d
```

### Local Development (Infrastructure Only)
1. Clone the repository.
2. Set up environment variables: `cp .env.example .env`.
3. Start local infrastructure: `docker compose up -d db mail`.
4. Run the application: `./gradlew bootRun`.
5. Access the application:
    - **API**: `http://localhost:8080`
    - **Swagger UI**: `http://localhost:8080/swagger-ui.html`
    - **Mailpit (Local Emails)**: `http://localhost:8025`

## Gradle Lifecycle

The build lifecycle is designed to be explicit.

### Primary Tasks
- `./gradlew ciBuild`: Deterministic CI/CD pipeline task (KtLint -> Detekt -> Test -> Kover -> BootJar).
- `./gradlew clean`: Deletes the build directory.
- `./gradlew compileKotlin`: Compiles the source code.
- `./gradlew test`: Runs the unit and integration test suite.
- `./gradlew ktlintCheck`: Validates Kotlin code style.
- `./gradlew detekt`: Performs static code analysis.
- `./gradlew koverHtmlReport`: Generates a coverage report in `build/reports/kover/html/index.html`.
- `./gradlew bootJar`: Assembles the executable JAR.

## Quality Gates & CI

The CI/CD pipeline (`.github/workflows/ci.yml`) enforces the following quality gates:

1.  **Validation**: `ktlintCheck` -> `detekt` -> `test` -> `koverVerify` -> `bootJar`.
2.  **Artifact Publication**: If the `main` branch passes validation, a Docker image is built and pushed to **GitHub Container Registry (GHCR)**.
    - Tags: `latest`, `sha-{commit-hash}`.

### The Canonical Gate
```bash
./gradlew ciBuild
```

The `Dockerfile` also uses the `ciBuild` task to ensure that no image is built with failing tests or quality gate violations.

## Documentation
- [Local Development Guide](docs/LOCAL_DEVELOPMENT.md)
- [Testing Strategy](docs/TESTING.md)
- [CI/CD Pipeline](docs/CI_CD.md)
- [Security & RBAC Model](docs/security-rbac.md)
- [Release Checklist](docs/RELEASE_CHECKLIST.md)
