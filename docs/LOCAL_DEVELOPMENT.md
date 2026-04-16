# Local Development Guide

## Setup

1. **JDK 17**: Ensure you have JDK 17 installed and `JAVA_HOME` set.
2. **Docker**: Ensure Docker is running to support local infrastructure via Docker Compose and integration tests via Testcontainers.
3. **Environment**: Copy `.env.example` to `.env`. Adjust `DB_HOST` and `SMTP_HOST` if you are running the app outside of Docker.

## Local Infrastructure

We use Docker Compose to manage local dependencies (PostgreSQL and Mailpit).

### Start the infrastructure (Database & Mail)
```bash
docker compose up -d db mail
```
This will start:
- **PostgreSQL 16** (Port 5432, with healthcheck)
- **Mailpit** (SMTP Port 1025, Web UI Port 8025)

### Start the full stack (Infrastructure + App)
```bash
docker compose up -d
```
This will build the application image and start it alongside the infrastructure.

You can verify the status with:
```bash
docker compose ps
```
Ensure `gayakini-db` is `healthy` before starting the application.

### Service Table
| Service | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------|
| Postgres| 5432          | 5432          | Primary Database |
| Mailpit | 1025          | 1025          | SMTP Server |
| Mailpit | 8025          | 8025          | Email Web UI |

### Stop the stack
```bash
docker compose down
```

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
4.  **Kover**: Coverage verification (Current baseline: 35%).
5.  **BootJar**: Assembles the executable JAR.

#### Running tests without Testcontainers
If you are on a system where Testcontainers is slow or unsupported, you can use the local Docker Compose database:
```bash
./gradlew ciBuild -Dtestcontainers.enabled=false
```
See [Testing Strategy](TESTING.md) for more details.

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
