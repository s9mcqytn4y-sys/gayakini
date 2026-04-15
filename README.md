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
- Docker (for PostgreSQL)

### Local Development
1. Clone the repository.
2. Set up environment variables in `.env` (use `.env.example` as a template).
3. Run the application: `./gradlew bootRun`.

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

The `ciBuild` task enforces the following quality gates before allowing a production-ready artifact to be generated:

1.  **KtLint Check**: Code must follow the established Kotlin coding style.
2.  **Detekt**: Static analysis must pass without any rule violations (fails on any error).
3.  **Unit & Integration Tests**: All tests must pass. Silent output on success, short stack traces on failure.
4.  **Kover Coverage**: A minimum of **80% instruction coverage** is required.
    -   *Exclusions*: Application entry points, DTOs, and infrastructure configurations.
5.  **Artifact Assembly**: Only if all previous steps pass, the `bootJar` is generated.

The `Dockerfile` also uses the `ciBuild` task to ensure that no image is built with failing tests or quality gate violations.

## Documentation
- [Local Development Guide](docs/LOCAL_DEVELOPMENT.md)
- [Testing Strategy](docs/TESTING.md)
- [Security & RBAC Model](docs/security-rbac.md)
- [Release Checklist](docs/RELEASE_CHECKLIST.md)
