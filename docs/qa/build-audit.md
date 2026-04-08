# Build Failure Audit - Phase 1

## Failure Categories & Root Cause Analysis

| Task | Symptom | Primary/Secondary | Root Cause |
| :--- | :--- | :--- | :--- |
| `:detekt` | 19 weighted issues | **Primary** | Real source code violations (MagicNumbers, ConstructorNaming, etc.). |
| `:jar` | Missing compiled path | **Secondary** | Likely caused by `:compileKotlin` failing in a previous run, leaving no classes for the JAR task. |
| `:ktlint...` | Missing report bin | **Secondary** | Gradle state inconsistency or task interruption during a failed build. |
| `:clean` | File locking (`build/`) | **Transient** | Windows file-locking issue where a process (e.g., Gradle daemon or IDE) holds a handle to `build/resolvedMainClassName`. |

## Root Cause Map

1.  **Detekt Violations (Primary):**
    *   **ConstructorParameterNaming:** Some classes use unconventional naming in constructors.
    *   **MagicNumber:** Use of literal numbers (e.g., status codes, business logic constants) instead of named constants.
    *   **TooManyFunctions / TooManyThrowStatements:** High complexity in service/controller layers.
    *   **UseCheckOrError:** `throw IllegalStateException(...)` should be replaced with `check(...)` or `error(...)`.
    *   **UnusedPrivateProperty:** Dead code in controllers or services.
2.  **Build Infrastructure (Secondary):**
    *   The `:jar` and `:ktlint` failures are symptoms of an unstable build state caused by the primary code quality failures interrupting the task graph.
    *   The `:clean` failure is an environmental issue (Windows) often mitigated by stopping the Gradle daemon or ensuring no other processes are accessing the `build` folder.

## File-by-File Remediation Checklist (Detekt)

*   [ ] `src/main/kotlin/com/gayakini/catalog/api/ProductController.kt` - MagicNumbers, complexity.
*   [ ] `src/main/kotlin/com/gayakini/order/application/OrderService.kt` - `IllegalStateException` usage, method complexity.
*   [ ] `src/main/kotlin/com/gayakini/checkout/application/CheckoutService.kt` - `IllegalStateException` usage.
*   [ ] `src/main/kotlin/com/gayakini/cart/application/CartService.kt` - `IllegalStateException` usage.
*   [ ] `src/test/kotlin/com/gayakini/e2e/CartE2ETest.kt` - Possible MagicNumbers or naming issues.

## Triage Note
The build is fundamentally blocked by **code quality gates (Detekt)**. Once these are resolved, the secondary failures (`jar`, `ktlint`) are expected to disappear as the compilation and reporting pipeline will complete successfully. The `clean` failure should be handled by a "retry or kill daemon" strategy on Windows.

## Recommended Execution Order (Phase 2)
1.  Fix Detekt violations in code (prioritize `UseCheckOrError` and `MagicNumber`).
2.  Refactor excessive `throw` statements or complexity where found.
3.  Execute `./gradlew clean` (verify no locking).
4.  Execute `./gradlew qualityCheck` to confirm code health.
5.  Execute `./gradlew releaseCheck` to confirm E2E build success.
