# Local Development Guide

## Setup

1. **JDK 17**: Ensure you have JDK 17 installed and `JAVA_HOME` set.
2. **Docker**: Used for running the PostgreSQL database.
3. **Environment**: Copy `.env.example` to `.env` and adjust as needed.

## Running the Application

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Run with Gradle
```bash
./gradlew bootRun
```
The application will start with the `local` profile enabled.

## Code Quality & Verification

Before pushing any changes, ensure all quality gates pass:

### Fast Feedback Loop
```bash
./gradlew qualityGate
```
This runs `ktlintCheck`, `detekt`, and all tests.

### Full Pre-release Check
```bash
./gradlew releaseCheck
```
This runs `qualityGate` and verifies the artifact can be built (`bootJar`).

## Database Migrations
We use Flyway for database migrations. New migrations should be placed in `src/main/resources/db/migration/`.
The migrations are automatically applied on startup.
