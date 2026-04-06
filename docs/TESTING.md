# Testing Strategy & QA Automation

## Test Layers

### 1. Unit Tests
Located in `src/test/kotlin/com/gayakini/[domain]/application`.
- Focus: Business rules, mapping, and logic.
- Fast, no database dependency.
- Use `io.mockk` for stubbing dependencies.

### 2. Integration Tests
Located in `src/test/kotlin/com/gayakini/[domain]`.
- Focus: Repository integration and Controller-to-Service wiring.
- Uses `H2` (Postgres Mode) for speed or `Testcontainers` (Postgres) for parity.
- Profile: `test`

### 3. Security Tests
Located in `src/test/kotlin/com/gayakini/infrastructure/security`.
- Focus: Authentication, Authorization (RBAC), and CORS.

## Automation Workflow

### Local Verification
Run all quality checks before pushing:
```bash
./gradlew releaseCheck
```

### Smoke Testing
Run a quick health check of critical paths:
```bash
./gradlew smokeTest
```

### Mocking External Providers
For local development and testing, we use mocks for:
- **Midtrans**: Simulated through `PaymentService` logic or mock endpoints.
- **Biteship**: Simulated through `ShippingService` logic or mock endpoints.

## REST Client (.http)
We use IntelliJ IDEA / VS Code `.http` files for manual and automated endpoint testing.
See the `http/` directory for:
- `auth.http`: Registration and login flows.
- `cart.http`: Cart management.
- `checkout.http`: Shipping calculation and checkout.
- `order-flow.http`: Placing and viewing orders.
- `webhooks.http`: Simulating provider notifications.
- Canonical path prefix is `/v1`, not `/api/v1`.
