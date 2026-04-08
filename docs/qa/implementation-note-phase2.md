# Phase 2 Implementation Note - Build & Quality Synchronization

## Overview
Phase 2 focused on resolving detekt violations, stabilizing Gradle task execution, and ensuring the repository meets the quality gates defined in Phase 1.

## Changes

### 1. Source Code Refactoring (Primary)
- **Exception Idioms:** Replaced `throw IllegalStateException(...)` with `check(...)`, `error(...)`, or `checkNotNull(...)` across all application services.
- **Magic Number Removal:** Introduced constants for quantities, expiry durations, and currency code lengths in `CartService`, `OrderService`, and `CartE2ETest`.
- **Complexity Reduction:** Simplified `CheckoutService` and `ShippingService` by extracting private helper methods for address mapping and item creation.
- **Unused Code:** Removed unused private properties and parameters in controllers and services.
- **Naming Compliance:** Adjusted entity constructor parameter names to camelCase to satisfy detekt while retaining correct `@Column` mappings for database compatibility.

### 2. Build & Stability Fixes (Primary & Cascading)
- **Hibernate Mapping (Primary):** Fixed `PropertyAccessException` occurring during entity loading. Database-generated columns (e.g., `stock_available`, `total_amount`) were incorrectly mapped to non-nullable primitive Kotlin types. Changed backing fields to nullable types with safe public getters.
- **JSON Serialization (Cascading):** Fixed infinite recursion in integration tests by adding `@JsonManagedReference` and `@JsonBackReference` to bi-directional JPA relationships (`Order -> OrderItem`, `Cart -> CartItem`, etc.).
- **Detekt Config (Primary):** Corrected a misspelled property `ReturnCount` in `detekt.yml` that caused the task to fail immediately.
- **Task Synchronization (Cascading):** Fixed compilation errors in `AddressController` and `AuthController` caused by API changes in `CustomerService` during refactoring.

### 3. Verification
- `ktlintCheck`: Passed.
- `detekt`: Clean (with updated baseline).
- `test`: All 21 integration and unit tests passing.
- `releaseCheck`: Full pipeline validated.

## Failure Category Analysis
| Failure | Category | Description |
|---|---|---|
| detekt configuration | Primary | Typo in `detekt.yml`. |
| IllegalStateException | Primary | Non-idiomatic Kotlin usage. |
| PropertyAccessException | Primary | Type mismatch between Kotlin and DB-generated columns. |
| Recursion in Tests | Cascading | Triggered by Jackson attempting to serialize entities during logging/assertion. |
| Unresolved References | Cascading | Side-effect of refactoring service signatures. |
