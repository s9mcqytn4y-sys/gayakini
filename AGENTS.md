# Agent Guidelines: gayakini Backend

## Source of Truth Hierarchy
1. **Code Implementation** (Real logic)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Rules of Engagement
- **No Over-Engineering:** Stick to the Modular Monolith. No microservices, No Kafka, No Redis unless explicitly required for MVP.
- **Evidence-Based:** Never claim a build or test is passing without actually running the Gradle tasks.
- **Roll-Forward Only:** Do not edit existing migration files if they have been committed. Create a new `V[N]__...sql` file.
- **Idempotency is Non-Negotiable:** Place Order, Payment creation, and Webhook processing must be idempotent.
- **Security First:** Never store raw guest tokens. Use `HashUtils.sha256()`.
- **Consistency:** Ensure DTOs, Entities, and OpenAPI specs are aligned.

## RC2: Frontend Integration Contract
- **Auth:** Protected routes require `Authorization: Bearer <token>`. Sandbox token: `sandbox-test-token`.
- **Error Response:** Always RFC 7807 (ProblemDetail). Check `docs/API_ERROR_REFERENCE.md`.
- **Flow Docs:** Refer to `docs/FRONTEND_SANDBOX_INTEGRATION.md`.

## Sandbox Staging Readiness
- **Midtrans:** Use `MIDTRANS_SERVER_KEY` from sandbox. Webhook requires SHA-512 signature key.
- **Biteship:** Use `BITESHIP_API_KEY` from sandbox. Webhook requires matching event types.

## Development Verification Flow
Before finishing any task, run:
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew test`
5. `./gradlew build`
