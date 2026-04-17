# AI Agent Instructions for Gayakini Repository

## 🎯 Project Context
This repository contains the backend architecture for the **Gayakini** project, an e-commerce platform for industrial suppliers.

## 🛠️ Technology Stack & Constraints
When writing, refactoring, or reviewing code, strictly adhere to the following stack:
* **Language:** Kotlin 2.0.21
* **Framework:** Spring Boot 3.4.0
* **Runtime:** JDK 17
* **Database Migrations:** Flyway
* **Security:** Spring Security
* **Database:** PostgreSQL
* **Payment Integration:** Midtrans

*Do not suggest downgrading versions or introducing libraries outside of this core stack without explicit instruction.*

## 🏢 Domain Knowledge & RBAC
The system handles complex data management and standardization for Indonesian industrial suppliers.
* **Roles:** The system implements a 4-role RBAC model: `CUSTOMER`, `ADMIN`, `FINANCE`, and `OPERATOR`.
* **Data Processing:** Interactions with structured JSON objects and Excel data formats.
* **Standardization:** Use centimeter (cm) standards unless specified otherwise.
*   **Phase 9 Implementation**: Implementation of inventory movements, warehouse fulfillment, and centralized business monitoring (`OrderMetrics`). This includes `InventoryMovement` tracking across `STORAGE`, `PACKING`, and `EXTERNAL` locations.
*   **RBAC Boundaries**: Secured `/v1/operations/**` for `ADMIN` and `OPERATOR` roles.

## 📝 Coding Guidelines
1. **Kotlin Idioms:** Utilize Kotlin's modern features (null safety, extension functions, data classes) to keep the code concise and safe.
2. **Immutability:** Prefer `val` over `var` and immutable collections by default.
3. **Clean Architecture:** Keep business logic decoupled from framework-specific configurations.
4. **Quality Tools:** ktlint (12.1.1), detekt (1.23.8), and Kover (0.9.0) are enforced via Gradle.
5. **Testing Pyramid:**
   - Use `MockK` for unit tests.
   - Use `@WebMvcTest` for controller/security slices.
   - Use `Testcontainers` (PostgreSQL 17) for integration tests. **Never use H2.**
   - All PRs must pass `./gradlew ciBuild`.
   - **Dockerized Build**: The `Dockerfile` and `ciBuild` task support a `-PexcludeIntegration` flag to skip Docker-dependent tests in isolated build environments.
6. **Agent constraints:**
   - **Verification:** Always run `./gradlew ciBuild` (or `./gradlew ciBuild -PexcludeIntegration` if Docker is unavailable) before declaring a task complete.
   - **Test Logs:** Logs are suppressed on success. Failures use `SHORT` stack traces. Do not re-enable full traces.
   - **Integration:** Use Spring Boot 3.4 `@ServiceConnection` for Testcontainers.
   - **Kover Coverage**: Maintain a minimum coverage of **43%** (verified via `koverVerify`).
