# Local Development Guide

## Prerequisites
- **JDK 17+** (Amazon Corretto or Temurin recommended)
- **PostgreSQL 18+** (Running locally or via Docker)

## Initial Setup
1. Run the `localSetup` task to initialize your `.env` file:
   ```bash
   ./gradlew localSetup
   ```
2. Configure your `.env` file with your local database credentials if they differ from the defaults:
   - `DB_HOST=localhost`
   - `DB_PORT=5432`
   - `DB_USERNAME=postgres`
   - `DB_PASSWORD=password`

## Running the Application
Use the specialized `bootRunLocal` task. This task performs a "preflight check" (`doctor`) before starting the app to ensure your database is reachable.

```bash
./gradlew bootRunLocal
```

## Quality Gates
Before pushing any code, run the `releaseCheck` task:
```bash
./gradlew releaseCheck
```
This runs:
- `ktlintCheck`: Linting
- `detekt`: Static analysis
- `test`: Unit and Integration tests
- `verifyMigrations`: Flyway check

## Testing Endpoints
We provide a suite of `.http` files in the `http/` directory. These can be run directly from IntelliJ IDEA or VS Code (with REST Client extension).

1. Open `http/smoke.http`
2. Select the `local` environment from `http/env/local.http.env.json`
3. Execute the requests.

## Database Migrations
To check the status of your migrations:
```bash
./gradlew verifyMigrations
```
To run migrations manually:
```bash
./gradlew flywayMigrate
```
