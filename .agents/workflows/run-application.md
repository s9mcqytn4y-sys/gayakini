---
description: Run the gayakini application locally with the local Spring profile
---

# Run Gayakini Application

Starts the gayakini Spring Boot application with local profile and database connectivity.
Equivalent of the VSCode launch config "GayakiniApplication".

## Prerequisites

- PostgreSQL running on `localhost:5432` with database `gayakini`
- `.env` file present (run `./gradlew localSetup` if missing)
- JDK 17 available

## Steps

1. Run the dbDoctor diagnostic first to verify prerequisites and seed data:
```shell
.\gradlew.bat dbDoctor --console=plain
```

2. Start the application with Gradle bootRun:
```shell
.\gradlew.bat bootRun --console=plain
```

The `bootRun` task automatically:
- Loads env vars from `.env`
- Sets `spring.profiles.active=local`
- Runs in local environment mode

## Endpoints after startup

| Endpoint | URL |
|---|---|
| API Base | http://localhost:8080/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Health | http://localhost:8080/actuator/health |

## Quick Smoke Test

After the server is running, verify key endpoints:
```shell
.\gradlew.bat releaseCheck --console=plain
```
