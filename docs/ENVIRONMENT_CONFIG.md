# Environment & Configuration Strategy

## Overview
Gayakini uses a layered Spring Boot profile approach to separate infrastructure defaults, developer convenience, and production requirements.

## Profile Layers

1.  **`application.yml` (Base Layer)**:
    - Contains neutral defaults (ports, JPA settings, Actuator exposure).
    - **Rule**: No database credentials or secrets allowed here.
    - **Rule**: Safe for all environments.
    - **Active Profile**: Controlled via `${SPRING_PROFILES_ACTIVE}`.

2.  **`application-local.yml` (Host Development)**:
    - Targeted at local development where the app runs on the host and infra in Docker.
    - Connects to `localhost:5432`.
    - **Rule**: Default profile when running `./gradlew bootRun` without env vars.

3.  **`application-dev.yml` (Containerized Dev)**:
    - Targeted at running the entire stack in Docker.
    - Connects to `db:5432` and `mail:1025`.
    - **Rule**: Used in `docker-compose.dev.yml`.

4.  **`application-staging.yml` (Pre-prod simulation)**:
    - Strict requirements. No default values for infrastructure or secrets.
    - **Rule**: Strictly reads from environment variables.
    - **Rule**: Validates production-like configuration integrity.

## Environment Matrix

| Feature             | Local (Host)          | Dev (Docker)          | Staging (Sim)         |
|---------------------|-----------------------|-----------------------|-----------------------|
| **Profile**         | `local`               | `dev`                 | `staging`             |
| **DB Host**         | `localhost`           | `db` (container)      | External / Env        |
| **Mail Host**       | `localhost` (Mailpit) | `mail` (Mailpit)      | External / Env        |
| **Memory (JVM)**    | 20% - 50% RAM         | 25% - 60% RAM         | 25% - 70% RAM         |
| **Secrets Source**  | `.env`                | `.env` / Compose      | Environment Vars      |

## JVM Memory Management
Memory policies are enforced via `JAVA_TOOL_OPTIONS`. 

- **Local**: `-XX:InitialRAMPercentage=20 -XX:MaxRAMPercentage=50`
- **Dev**: `-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=60`
- **Staging**: `-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=70`

## Environment Variables
All secrets and environment-specific overrides must be provided via environment variables.

| Variable | Description | Profile |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profiles (e.g., `local`, `prod`) | All |
| `DB_HOST` | Database hostname | All |
| `DB_PASSWORD` | Database password | All |
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | All |
| `MIDTRANS_SERVER_KEY` | Midtrans API Server Key | sandbox/prod |

## Security Rules
- **No Hardcoded Secrets**: Critical properties in `GayakiniProperties.kt` must not have default values that could be used in production.
- **Fail Fast**: The application should fail to start if a mandatory secret is missing in a production-like profile.
- **Validation**: All configuration properties are validated at startup using `@Validated`.
