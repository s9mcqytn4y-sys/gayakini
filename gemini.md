# Gemini Agent Context: gayakini

`AGENTS.md` adalah source of truth operasional. File ini hanya overlay Gemini dan tidak boleh bertentangan dengannya.

You are an expert Kotlin and Spring Boot developer.
Repository Root: `C:\Software\gayakini`

## Operational Context
- **Backend:** Spring Boot 3.4, Kotlin 2.0.
- **Database:** PostgreSQL with Flyway migrations.
- **Tools:** Gradle, ktlint, detekt.
- **Key Integrations:** Midtrans (Payments), Biteship (Logistics).

## MCP Server Rules
1. **gayakini-filesystem:**
   - Always restrict reads/writes to `C:\Software\gayakini`.
   - **AIDA CRITICAL:** Use ONLY `{"path": "..."}` for `read_file`. No extra keys.
   - Validate launcher: `./gradlew validateMcp` or `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly`
2. **gayakini-http:**
   - Default ke `APP_BASE_URL=http://localhost:8080`.
   - Default spec adalah `docs\brand-fashion-ecommerce-api-final.yaml`.
   - Treat as local OpenAPI helper, not production API bridge.
3. **gayakini-browser:**
   - Package target adalah Puppeteer MCP server.
   - Tidak ada native browser-type flag; Edge hanya via executable path ke `msedge.exe`.
   - Validate launcher: `./gradlew validateMcp`
4. **gayakini-github:**
   - **AIDA CRITICAL:** Use ONLY minimal keys for `create_or_update_file`: `owner`, `repo`, `path`, `content`, `message`.
   - Token precedence: `GITHUB_PERSONAL_ACCESS_TOKEN`, `GITHUB_TOKEN`, `GH_TOKEN`.
   - Prefer read/query oriented workflows.
5. **gayakini-terminal:**
   - Use for `./gradlew` commands.
   - Prefer `./gradlew releaseCheck` for core evidence (Clean + Quality + Build + MCP).
   - Use `./gradlew releaseCheckLocal` if DB validation is needed.
6. **gayakini-postgres:**
   - Use to verify schema in `commerce` or `public` schemas.
   - Do not perform destructive `DROP` or `TRUNCATE` without confirmation.
   - Local defaults: `localhost:5432`, database `gayakini`, user `postgres`, password `password`.
7. **Launcher behavior:**
   - All launcher scripts live under `tooling\mcp`.
   - For Codex local MCP, use Windows PowerShell directly, not WSL.
   - Use `-ValidateOnly` or `./gradlew validateMcp` for smoke checks.
   - After launcher changes, sync `AGENTS.md`, provider overlays, and docs under `docs/tooling`.

## Coding Standards
- Use `UUIDv7` for primary keys (via `com.github.f4b6a3:uuid-creator`).
- Domain logic belongs in `application` or `domain` packages.
- Entities must use JPA annotations correctly with Kotlin.
- Ensure all new API endpoints are documented in OpenAPI/Swagger.

## Antigravity IDE Workflows
Equivalent workflows tersedia di `.agents/workflows/`:
- `/validate-mcp-launchers` — MCP launcher preflight (via Gradle)
- `/docs-parity-check` — Doc parity check
- `/mcp-hardening-preflight` — Full MCP hardening
- `/gradle-release-verification` — Full Gradle quality gate via `releaseCheck`
- `/run-application` — Jalankan server lokal via `bootRun`

## Verification Workflow
Before task completion:
1. Run `./gradlew clean`
2. Run `./gradlew releaseCheck` (Core quality gate: Lint + Detekt + Tests + MCP Validation)
3. Run `./gradlew dbDoctor` to ensure local DB state is healthy if relevant.

## Maintenance Notes
- Treat `AGENTS.md` as authoritative.
- Keep Gemini-specific guidance additive, not divergent.
- Jika `.agents/workflows/` berubah, sinkronkan ke `AGENTS.md` dan overlay provider.
- If Gradle tasks change (e.g., `doctor` -> `dbDoctor`), update this file immediately.

## Troubleshooting Workflow
1. Start from `./gradlew validateMcp`.
2. For Gradle stuck, use `./gradlew dbDoctor` to check PostgreSQL auto-start/state.
3. For HTTP issues, verify `APP_BASE_URL` and `OPENAPI_SPEC_PATH`.
4. For browser issues, verify executable path assumptions first.
5. For GitHub issues, check token source only, never commit token values.
