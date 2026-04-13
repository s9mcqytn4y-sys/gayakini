# Gayakini Backend

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
3. Start the database: `docker-compose up -d`.
4. Run the application: `./gradlew bootRun`.

## Gradle Lifecycle & Quality Gates

The build lifecycle is designed to be explicit and strictly enforced.

### Primary Tasks
- `./gradlew clean`: Deletes the build directory.
- `./gradlew compileKotlin`: Compiles the source code.
- `./gradlew test`: Runs the unit and integration test suite.
- `./gradlew ktlintCheck`: Validates Kotlin code style.
- `./gradlew detekt`: Performs static code analysis.
- `./gradlew qualityGate`: Runs `ktlintCheck`, `detekt`, and `test`. Use this for fast local feedback.
- `./gradlew bootJar`: Assembles the executable JAR.
- `./gradlew build`: Assembles and tests the project.
- `./gradlew releaseCheck`: Full project validation (qualityGate + bootJar). Run this before pushing.

## Documentation
- [Local Development Guide](docs/LOCAL_DEVELOPMENT.md)
- [Testing Strategy](docs/TESTING.md)
- [Release Checklist](docs/RELEASE_CHECKLIST.md)
