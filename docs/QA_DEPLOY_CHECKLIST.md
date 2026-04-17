# Gayakini Backend QA & Deploy-Readiness Checklist

This document provides a comprehensive checklist for ensuring the Gayakini backend is secure, performant, and correctly configured before any production deployment.

## 1. Environment & Infrastructure Verification
- [ ] **Environment Parity**: Verify that `SPRING_PROFILES_ACTIVE` is set to `staging` or `prod`.
- [ ] **Secrets Management**: Ensure no secrets (DB_PASSWORD, JWT_SECRET, MIDTRANS_SERVER_KEY) are hardcoded or committed to version control.
- [ ] **Database Readiness**:
    - [ ] PostgreSQL 17 is running and accessible.
    - [ ] Flyway migrations are up-to-date (`./gradlew flywayInfo`).
    - [ ] Database backups and retention policies are configured.
- [ ] **JVM Memory Hardening**:
    - [ ] `JAVA_TOOL_OPTIONS` includes percentage-based RAM limits (e.g., `-XX:MaxRAMPercentage=75`).
    - [ ] Garbage Collector is appropriate for the workload (G1GC for JVM 17+).

## 2. Quality & Stability Gates
- [ ] **Build Status**: `./gradlew ciBuild` passes consistently on the target branch.
- [ ] **Linting & Quality**:
    - [ ] `ktlint` passes without errors.
    - [ ] `detekt` passes with zero weighted issues.
- [ ] **Test Coverage**:
    - [ ] `koverVerify` confirms coverage is >= 43%.
    - [ ] Unit tests (MockK) cover critical business logic (Order, Payment, Inventory).
    - [ ] Integration tests (Testcontainers) verify DB and external service interaction.

## 3. Security & Compliance
- [ ] **RBAC Enforcement**:
    - [ ] `/v1/admin/**` restricted to `ADMIN`.
    - [ ] `/v1/finance/**` restricted to `ADMIN`, `FINANCE`.
    - [ ] `/v1/operations/**` restricted to `ADMIN`, `OPERATOR`.
- [ ] **Webhook Integrity**:
    - [ ] Midtrans signature validation is active and verified.
    - [ ] Biteship signature validation is configured.
- [ ] **Rate Limiting**: `Bucket4j` limits are configured for `/v1/auth`, `/v1/checkouts`, and `/v1/webhooks`.
- [ ] **Input Validation**: All `@RestController` methods use `@Valid` or `@Validated` on request bodies.

## 4. Observability & Monitoring
- [ ] **Logging Strategy**:
    - [ ] Request IDs are propagated via MDC.
    - [ ] Logback is configured with appropriate levels (INFO/WARN for prod).
- [ ] **Health Checks**:
    - [ ] `/actuator/health` is public but detailed info is restricted to `ADMIN`.
    - [ ] External provider indicators (Midtrans, Biteship) are functioning.
- [ ] **Business Metrics**:
    - [ ] `gayakini.orders.paid` and `gayakini.payments.failed` counters are visible in `/actuator/prometheus`.

## 5. Deployment & Runtime
- [ ] **Containerization**:
    - [ ] Docker image is multi-stage and uses a non-root user (`spring`).
    - [ ] `docker-compose.yml` profiles are correctly segregated.
- [ ] **Timeout Policies**: `RestTemplate` has explicit connect and read timeouts (e.g., 5s/10s).
- [ ] **Cors Policy**: `CORS_ALLOWED_ORIGINS` is restricted to authoritative domains.

## 6. Post-Deployment Smoke Tests
- [ ] [ ] Health check returns `UP`.
- [ ] [ ] Public product listing API works.
- [ ] [ ] Login/Register flows are functional.
- [ ] [ ] Test webhook callback (if possible in staging).
