# Architectural Gap Report: Midtrans Snap Integration

## 1. Executive Summary
This report identifies the gaps between the current Gayakini repository state and the target "Midtrans Snap Sandbox-First" rollout.

## 2. Current State Analysis
### What Exists
- **Infrastructure:** Basic Spring Boot 3.4 + Kotlin 2.0 setup.
- **Database:** PostgreSQL schema defined with `payments` and `payment_status_histories` tables.
- **Properties:** `GayakiniProperties` with `MidtransProperties` and `BiteshipProperties`.
- **Placeholder Logic:** `PaymentService` exists but likely needs hardening.
- **Documentation:** `SANDBOX_PAYMENT_FLOW.md` exists but may not be fully synced with implementation.

### What is Missing
- **Strict Validation:** No fail-fast startup validator for payment environment variables.
- **Sandbox Profile:** Lack of a dedicated `application-sandbox.yml` for clean environment separation.
- **E2E Webhook Security:** Webhook signature verification needs to be robustly implemented.
- **Idempotency Hardening:** While mentioned in docs, the implementation in `PaymentService` needs verification for concurrent requests.

## 3. Gap Details
### G1: Environment Safety
Current configuration allows the application to start even with dummy or potentially production keys if misconfigured. There is no active check to prevent "sandbox" profile from connecting to Midtrans production endpoints.

### G2: Profile Consistency
Configurations are scattered between `local`, `docker`, and `.env.example`. A unified `sandbox` profile strategy is needed.

### G3: Biteship Integration
Schema exists, but application logic for Biteship is non-existent. This is deferred as per ADR 0001.

## 4. Remediation Plan
1. **Normalize Configuration:** Align `application.yml` and create `application-sandbox.yml`.
2. **Implement Fail-Fast Validator:** Add a startup check for Midtrans properties.
3. **Lockdown Profiles:** Ensure the application default or sandbox mode is truly safe.
