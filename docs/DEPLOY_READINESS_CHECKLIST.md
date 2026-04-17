# QA Deploy-Readiness Checklist (Host-Ready)

This document serves as the repository's source of truth for determining if a build is ready for deployment to Staging or Production environments.

- [ ] **ciBuild Pass**: `./gradlew ciBuild` passes 100% on the `main` branch.
- [ ] **PostgreSQL Version**: Host/Container running **PostgreSQL 17.x** (alpine preferred).
- [ ] **Flyway Migration Safety**: 
    - [ ] `baseline-on-migrate` is enabled.
    - [ ] All migrations in `src/main/resources/db/migration` are idempotent.
    - [ ] No destructive `DROP` or `RENAME` without a backward-compatible transition.

## 2. Profile & Config Readiness
- [ ] **Profile Mapping**: Active profile matches the target environment (`staging` or `prod`).
- [ ] **Secrets Coverage**: All variables in `.env.example` are accounted for in the target host/container environment.
- [ ] **No Local Defaults**: `application-staging.yml` (or prod) does not rely on `localhost` for external services.
- [ ] **JVM Memory Management**: `JAVA_TOOL_OPTIONS` configured with `InitialRAMPercentage` and `MaxRAMPercentage`.
- [ ] **OOM Handling**: `-XX:+ExitOnOutOfMemoryError` is present to ensure container restart.

## 3. Container & Artifact Validity
- [ ] **Docker Build**: `docker build .` succeeds (internal `ciBuild` must pass).
- [ ] **Artifact Selection**: `bootJar` produces `app.jar` as the entry point.
- [ ] **Image Tagging**: Image is tagged with `sha-{commit}` and `latest`.
- [ ] **Health Checks**: `docker inspect` shows the container as `healthy`.
- [ ] **Readiness Probe**: `/actuator/health/readiness` returns `UP` only after migrations.

## 4. Security & Infrastructure Baseline
- [ ] **Connectivity**: App can reach DB and SMTP host from within the target network.
- [ ] **Rate Limiting**: Enabled and verified for Auth (10/min) and Checkout (20/min) paths.
- [ ] **Webhook Verification**: Signatures verified using constant-time comparison (Biteship/Midtrans).
- [ ] **Logging Policy**: Rolling file appender configured (30-day/3GB retention) for `dev`, `staging`, and `prod`.
- [ ] **Secrets Handling**: No plain-text secrets in logs; Actuator endpoints (except health/info) are secured.

## 5. Smoke Test (Critical Path)
- [ ] **Auth**: Login and token issuance work.
- [ ] **Catalog**: Product listing and search return data.
- [ ] **Order**: Checkout flow (mocked payment) completes successfully.
- [ ] **Webhooks**: Mocked notification accepted with `200 OK`.

## 6. Rollback & Maintenance
- [ ] **Rollback Readiness**: Previous stable image tag is known and pullable.
- [ ] **Database Reversion**: Rollback script or manual steps for the current migration are documented.
- [ ] **Maintenance Mode**: Strategy for zero-downtime or scheduled maintenance is defined.
