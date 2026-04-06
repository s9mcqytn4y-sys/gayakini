# Agent Guidelines: gayakini Backend

## Repository Root: `C:\Software\gayakini`

## CRITICAL: Security & Secret Scanning
- **DO NOT COMMIT SECRETS:** File `.env`, `local.env`, dan file environment lainnya terlarang untuk masuk ke Git.
- **Git Hygiene:** Jika terjadi kebocoran secret ke remote repository, segera rotate key tersebut di provider terkait.

## CRITICAL: AIDA / MCP Tool Rules
Saat menggunakan MCP tools, gunakan input seminimal mungkin.
- **`read_file`:** Cukup `{"path": "..."}`.
- **`create_or_update_file`:** Gunakan hanya `owner`, `repo`, `path`, `content`, `message`.
- **Windows-first MCP:** Untuk launcher lokal, gunakan `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-*.ps1`.
- **Preflight launcher:** Gunakan `-ValidateOnly` dulu sebelum wiring ke Codex atau VS Code task.

## Source of Truth Hierarchy
1. **Code Implementation** (Kotlin/Java)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Rules of Engagement
- **Architecture:** Modular Monolith. Tetap sederhana, hindari over-engineering.
- **Evidence-Based:** Jangan berasumsi build/test sukses tanpa menjalankan perintah Gradle.
- **Database:** Gunakan `UUIDv7`. Jangan edit file migrasi yang sudah rilis (Commit baru untuk perubahan).
- **Idempotency:** Wajib untuk Place Order, Payment, dan Webhook processing.

## Development Verification Flow
Gunakan `gayakini-terminal` untuk verifikasi:
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew test`
5. `./gradlew build`
6. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly`
7. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-postgres.ps1 -ValidateOnly`
8. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-github.ps1 -ValidateOnly`
9. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-git.ps1 -ValidateOnly`
10. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-terminal.ps1 -ValidateOnly`

## Local MCP Defaults
- `PROJECT_ROOT` / `REPO_ROOT`: default ke `C:\Software\gayakini`
- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=gayakini`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=password`
- `GITHUB_PERSONAL_ACCESS_TOKEN` diprioritaskan, fallback ke `GITHUB_TOKEN` atau `GH_TOKEN`
