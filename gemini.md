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
   - Validate launcher first: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly`
2. **gayakini-http:**
   - Default ke `APP_BASE_URL=http://localhost:8080`.
   - Default spec adalah `docs\brand-fashion-ecommerce-api-final.yaml`.
   - Treat as local OpenAPI helper, not production API bridge.
3. **gayakini-browser:**
   - Package target adalah Puppeteer MCP server.
   - Tidak ada native browser-type flag; Edge hanya via executable path ke `msedge.exe`.
   - Validate launcher first: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-browser.ps1 -ValidateOnly`
4. **gayakini-github:**
   - **AIDA CRITICAL:** Use ONLY minimal keys for `create_or_update_file`: `owner`, `repo`, `path`, `content`, `message`.
   - Token precedence: `GITHUB_PERSONAL_ACCESS_TOKEN`, `GITHUB_TOKEN`, `GH_TOKEN`.
   - Prefer read/query oriented workflows.
5. **gayakini-terminal:**
   - Use for `./gradlew` commands.
   - Prefer `./gradlew clean ktlintCheck detekt test build` for evidence.
6. **gayakini-postgres:**
   - Use to verify schema in `public` or `history` schemas.
   - Do not perform destructive `DROP` or `TRUNCATE` without confirmation.
   - Local defaults: `localhost:5432`, database `gayakini`, user `postgres`, password `password`.
7. **Launcher behavior:**
   - All launcher scripts live under `tooling\mcp`.
   - For Codex local MCP, use Windows PowerShell directly, not WSL.
   - Use `-ValidateOnly` for smoke checks because real MCP launchers must keep STDIO clean.
   - After launcher changes, sync `AGENTS.md`, provider overlays, and docs under `docs/agents` / `docs/tooling`.

## Coding Standards
- Use `UUIDv7` for primary keys (via `com.github.f4b6a3:uuid-creator`).
- Domain logic belongs in `application` or `domain` packages.
- Entities must use JPA annotations correctly with Kotlin.
- Ensure all new API endpoints are documented in OpenAPI/Swagger.

## Antigravity IDE Workflows
Equivalent workflows tersedia di `.agents/workflows/`:
- `/validate-mcp-launchers` — MCP launcher preflight
- `/docs-parity-check` — Doc parity check
- `/mcp-hardening-preflight` — Full MCP hardening
- `/gradle-release-verification` — Full Gradle quality gate
- `/run-application` — Jalankan server lokal

## Verification Workflow
Before task completion:
1. Run `./gradlew clean`
2. Run `./gradlew ktlintCheck`
3. Run `./gradlew detekt`
4. Run `./gradlew test`
5. Run `./gradlew build`
6. Validate each launcher in `tooling\mcp\start-*.ps1` with `-ValidateOnly`

## Maintenance Notes
- Treat `AGENTS.md` as authoritative.
- Keep Gemini-specific guidance additive, not divergent.
- Jika `.agents/workflows/` berubah, sinkronkan ke `AGENTS.md` dan overlay provider.
- If HTTP or browser assumptions change, update this file together with `AGENTS.md` and `docs/tooling/mcp-servers.md`.

## Troubleshooting Workflow
1. Start from `-ValidateOnly`.
2. For HTTP issues, verify `APP_BASE_URL` and `OPENAPI_SPEC_PATH`.
3. For browser issues, verify executable path assumptions first; Edge requires `msedge.exe` path, not a native browser-type flag.
4. For GitHub issues, check token source only, never commit token values.

## Release / Hardening Workflow
1. Review launcher/helper/doc/workspace/workflow changes as one small package.
2. Re-run all launcher preflight checks after shared helper changes.
3. Keep this overlay aligned with `AGENTS.md`; do not leave divergent provider instructions behind.
