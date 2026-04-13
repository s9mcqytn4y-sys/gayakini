# Release Checklist

This document outlines the mandatory steps to follow before merging to `main` and deploying to production.

## 1. Local Verification
Before pushing any code, you must run the following Gradle task to ensure all quality gates are met:

```bash
./gradlew releaseCheck
```
This task verifies:
- **Code Style**: `ktlintCheck` passes.
- **Static Analysis**: `detekt` passes.
- **Tests**: All unit and integration tests pass.
- **Buildability**: The project can be successfully packaged into a JAR (`bootJar`).

## 2. Dependency Audit
- No new libraries added without architectural review.
- All dependencies are on their latest stable versions.

## 3. Database Migrations
- Flyway migrations are verified against a local PostgreSQL instance.
- Migrations are idempotent and safe for production rollback if applicable.

## 4. API Documentation
- Ensure OpenAPI documentation is up to date.
- Verify `HelloController` or relevant health endpoints.

## 5. Security Check
- Ensure no secrets are committed (check `.env` and `.env.example`).
- JWT and authentication filters are properly configured for the target environment.
