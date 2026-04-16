# Environment & Configuration Strategy

## Overview
Gayakini uses a layered Spring Boot profile approach to separate infrastructure defaults, developer convenience, and production requirements.

## Profile Layers

1.  **`application.yml` (Base Layer)**:
    - Contains neutral defaults (ports, JPA settings, Actuator exposure).
    - **Rule**: No database credentials or secrets allowed here.
    - **Rule**: Safe for all environments.

2.  **`application-local.yml` (Developer Layer)**:
    - Targeted at local development via Docker Compose or local installs.
    - Contains "safe" defaults like `localhost` and dummy secrets.
    - **Rule**: Never used in production.

3.  **`application-prod.yml` (Production Template)**:
    - Strict requirements.
    - **Rule**: No defaults for secrets. Every sensitive value must be a placeholder like `${SECRET_NAME}`.
    - **Rule**: Fails fast if required environment variables are missing.

4.  **`application-docker.yml`**:
    - Overrides for running the app *inside* the Docker network (e.g., `db` instead of `localhost`).

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
