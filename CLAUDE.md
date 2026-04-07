# Claude Overlay: gayakini

`AGENTS.md` adalah source of truth operasional repo ini. `CLAUDE.md` hanya overlay provider-specific untuk Claude dan tidak boleh bertentangan dengannya.

## Operational Context
- **Repository Root:** `C:\Software\gayakini`
- **Stack:** Spring Boot 3.4, Kotlin 2.0, PostgreSQL, Flyway.

## MCP Set
- `gayakini-filesystem`: Repo file access.
- `gayakini-postgres`: Local DB schema & query.
- `gayakini-github`: GitHub API (query-focused).
- `gayakini-git`: Local git operations.
- `gayakini-terminal`: Command execution (Gradle/Shell).
- `gayakini-http`: Local API helper (OpenAPI driven).
- `gayakini-browser`: Puppeteer automation.

## MCP Launcher Rules
- Gunakan launcher lokal berbasis PowerShell Windows: `tooling\mcp\start-*.ps1`.
- Selalu gunakan `-ValidateOnly` atau `./gradlew validateMcp` sebelum wiring ke client.
- Browser: Edge didukung via `msedge.exe` path (tidak ada flag native).
- HTTP: Default ke `http://localhost:8080` dan spec lokal.

## Gradle Tasks (New Matrix)
- `./gradlew qualityCheck`: Lint + Detekt + Unit Tests (CI Gate).
- `./gradlew releaseCheck`: Core release gate (Clean + Quality + Build + MCP). **Safe from local DB stuck.**
- `./gradlew releaseCheckLocal`: releaseCheck + Local DB migration validation.
- `./gradlew dbDoctor`: Database & environment diagnostic.
- `./gradlew dbStart`: Attempt auto-start PostgreSQL (Windows).

## Antigravity IDE Workflows
Equivalent workflows tersedia di `.agents/workflows/`:
- `/validate-mcp-launchers` — MCP launcher preflight (via Gradle)
- `/docs-parity-check` — Doc parity check
- `/mcp-hardening-preflight` — Full MCP hardening
- `/gradle-release-verification` — Full Gradle quality gate via `releaseCheck`
- `/run-application` — Jalankan server lokal via `bootRun`

## Verification Workflow
1. Run `./gradlew qualityCheck` for code sanity.
2. Run `./gradlew validateMcp` for launcher sanity.
3. Use `./gradlew releaseCheck` for final evidence before completion.
4. If DB is involved, run `./gradlew dbDoctor` first.

## Maintenance Rule
- Sinkronkan `AGENTS.md`, overlay provider, dan `docs/tooling/mcp-servers.md` jika ada perubahan pada launcher atau task Gradle.
- Jangan simpan secret di repository.
