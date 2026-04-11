# Agent Guidelines: gayakini Backend

## Repository Root: `C:\Software\gayakini`

## MISSION: Core Build Foundation
The repository has been reset to a clean, modern Gradle baseline.
- **Priority:** Compilation, Static Analysis (Ktlint, Detekt), and clean Build Graph.
- **Simplified:** All legacy orchestration, manual DB helpers, and custom Windows-only scripts have been removed.

## CRITICAL: Security & Secret Scanning
- **DO NOT COMMIT SECRETS:** File `.env` and other environment files are strictly forbidden in Git.
- **Git Hygiene:** If a secret leaks, rotate it immediately.

## Source Of Truth Model
1. **Code Implementation** (Kotlin/Java)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Core Verification Flow
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew check` (Runs all quality guardrails)
5. `./gradlew build` (Assembles and verifies)

## Rules of Engagement
- **Architecture:** Modular Monolith.
- **Evidence-Based:** Always run Gradle commands to verify success.
- **Quality Gate:** All changes must pass `./gradlew check`.
- **K2 Compiler:** Kotlin 2.0.21 is active.

## Local Defaults
- `PROJECT_ROOT`: `C:\Software\gayakini`
- `DB_URL`: `jdbc:postgresql://localhost:5432/gayakini`
- `APP_BASE_URL`: `http://localhost:8080`
