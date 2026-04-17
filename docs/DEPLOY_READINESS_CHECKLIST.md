# QA Deploy-Readiness Checklist (Host-Ready)

This document serves as the repository's source of truth for determining if a build is ready for deployment to Staging or Production environments.

## 1. Environment & Profile Readiness
- [ ] **Profile Mapping**: Active profile matches the target environment (`staging` or `prod`).
- [ ] **No Local Defaults**: Verified that `application-staging.yml` (or prod) does not rely on any `localhost` or "dummy" defaults for external services.
- [ ] **Env Var Coverage**: All required variables in `.env.example` are accounted for in the target host/container environment.

## 2. Infrastructure & Database
- [ ] **PostgreSQL Version**: Host/Container running **PostgreSQL 17.x**.
- [ ] **Flyway Migration Safety**: 
    - [ ] `baseline-on-migrate` is enabled.
    - [ ] All migrations in `src/main/resources/db/migration` are idempotent.
    - [ ] No destructive `DROP` or `RENAME` without a backward-compatible transition.
- [ ] **Connectivity**: App can reach DB and SMTP host from within the target network.
- [ ] **Schema Readiness**: Default schema `commerce` exists and is accessible by the app user.

## 3. Container & Artifact Validity
- [ ] **Docker Build**: `docker build .` succeeds without using local cache for critical steps.
- [ ] **Image Tagging**: Image is tagged with `sha-{commit}` and `latest` (or semantic version).
- [ ] **Health Checks**: `docker inspect` shows the container as `healthy`.
- [ ] **Readiness Probe**: `/actuator/health/readiness` returns `UP` only after migrations and initial warm-up.

## 4. Quality & Security Gates
- [ ] **ciBuild Pass**: `./gradlew ciBuild` passes 100% on the `main` branch.
- [ ] **Kover Coverage**: Coverage meets or exceeds **42%**.
- [ ] **Security Baseline**: 
    - [ ] Rate limiting enabled and verified for Auth and Checkout paths.
    - [ ] Webhook signatures verified using constant-time comparison.
    - [ ] No plain-text secrets in logs.
    - [ ] Actuator endpoints (except health/info) are secured by `ADMIN` role.

## 5. Runtime Stability
- [ ] **Memory Management**: `JAVA_TOOL_OPTIONS` configured with `InitialRAMPercentage` and `MaxRAMPercentage`.
- [ ] **OOM Handling**: `-XX:+ExitOnOutOfMemoryError` is present to ensure container restart.
- [ ] **Log Rotation**: Rolling file appender configured for 30-day retention or 3GB cap.
- [ ] **Cache TTL**: Caffeine caches have explicit expiration policies to prevent memory leaks.

## 6. Smoke Test (Critical Path)
- [ ] **Auth**: Login and token issuance work.
- [ ] **Catalog**: Product listing and search return data.
- [ ] **Order**: Checkout flow (mocked payment) completes successfully.
- [ ] **Webhooks**: Mocked Midtrans/Biteship notification accepted with `200 OK`.

## 7. Rollback Readiness
- [ ] **Artifact Availability**: Previous stable image tag is known and pullable.
- [ ] **Database Reversion**: Rollback script or manual steps for the current migration are documented if necessary.
