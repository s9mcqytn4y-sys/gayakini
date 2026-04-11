# Agent Guidelines: gayakini Backend

## Repository Root: `C:\Software\gayakini`

## MISSION: Phase 1 Reset - Foundation First
The repository has been reset to a **Phase 1: Foundation** baseline.
- **Core Priority:** Compilation, Static Analysis (Ktlint, Detekt), and clean Build Graph.
- **Isolations:** Database orchestration, MCP validation, and legacy release scripts are isolated from the core build path.

## CRITICAL: Security & Secret Scanning
- **DO NOT COMMIT SECRETS:** File `.env`, `local.env`, and other environment files are strictly forbidden in Git.
- **Git Hygiene:** If a secret leaks, rotate it immediately.

## Source Of Truth Model
1. **Code Implementation** (Kotlin/Java)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Phase 1 Core Verification Flow
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew check` (Runs all quality guardrails)
5. `./gradlew build` (Assembles and verifies)

## Isolated Legacy Tasks (Non-Core)
The following tasks are moved to `legacy` group and are **NOT** part of the standard Phase 1 verification:
- `./gradlew dbDoctor` (Database diagnostics)
- `./gradlew validateMcp` (MCP launcher preflight)
- `./gradlew releaseCheck` (Legacy combined check)

## Rules of Engagement
- **Architecture:** Modular Monolith. Focus on core stability first.
- **Evidence-Based:** Always run Gradle commands to verify success.
- **Quality Gate:** All PRs must pass `./gradlew check`.
- **K2 Compiler:** Kotlin 2.0.21 is active.

## Local MCP Defaults
- `PROJECT_ROOT`: `C:\Software\gayakini`
- `DB_URL`: `jdbc:postgresql://localhost:5432/gayakini`
- `APP_BASE_URL`: `http://localhost:8080`
