# Local Development Guide

## Setup

1. **JDK 17**: Ensure you have JDK 17 installed and `JAVA_HOME` set.
2. **Docker**: Ensure Docker is running to support PostgreSQL 16 via Testcontainers.
3. **Environment**: Copy `.env.example` to `.env` and adjust as needed.

## Running the Application

### 1. Run with Gradle
```bash
./gradlew bootRun
```
The application will start with the `local` profile enabled.

## Code Quality & Verification

The project enforces strict quality gates before any code is considered ready for pull requests.

### The Canonical Gate
```bash
./gradlew ciBuild
```
This is the ultimate local verification. It runs:
1.  **KtLint Check**: Kotlin style validation.
2.  **Detekt**: Static code analysis.
3.  **Tests**: Unit and integration test suite.
4.  **Kover**: 80% instruction coverage verification.
5.  **BootJar**: Assembles the executable JAR.

### Test Coverage Report
To view the coverage report:
```bash
./gradlew koverHtmlReport
```
Open `build/reports/kover/html/index.html` in your browser.

### Test Logging Behavior
-   **Success**: Logs are intentionally suppressed on success to reduce terminal noise.
-   **Failure**: You will only see failures with `SHORT` stack traces.
-   **Summary**: A concise result summary is printed at the end.

## Database Migrations
We use Flyway for database migrations. New migrations should be placed in `src/main/resources/db/migration/`.
Migrations are automatically applied on startup.
