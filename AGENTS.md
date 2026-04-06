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
- **GitHub MCP:** Gunakan read/query oriented workflow. Hindari write/publish kecuali user minta eksplisit dan sudah diverifikasi.
- **HTTP MCP:** Default ke OpenAPI lokal repo + base URL aplikasi lokal. Jangan arahkan ke production API.
- **Browser MCP:** Untuk automation lokal saja. Jangan pakai launch options berisiko kecuali perlu dan user paham dampaknya.
- **Doc sync:** Setiap perubahan di `tooling/mcp/**`, `.vscode/**`, atau workflow validasi wajib disinkronkan ke dokumen agent/provider terkait pada turn yang sama.

## Source Of Truth Model
- `AGENTS.md` adalah operational source of truth untuk perilaku agent lokal di repo ini.
- `gemini.md`, `CLAUDE.md`, dan `CODEX.md` adalah provider-specific overlays. Overlay boleh menambah penekanan provider, tapi tidak boleh bertentangan dengan `AGENTS.md`.
- Dokumen rinci MCP lokal ada di `docs/tooling/mcp-servers.md`.
- Prompt pola operasi Codex ada di `docs/agents/codex-mcp-prompts.md`.

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
11. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-http.ps1 -ValidateOnly`
12. `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-browser.ps1 -ValidateOnly`
13. `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Get-Content .vscode\tasks.json | ConvertFrom-Json | Out-Null"`
14. `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Get-Content .github\workflows\mcp-launchers.yml | Out-Null"`

## Local MCP Defaults
- `PROJECT_ROOT` / `REPO_ROOT`: default ke `C:\Software\gayakini`
- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=gayakini`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=password`
- `GITHUB_PERSONAL_ACCESS_TOKEN` diprioritaskan, fallback ke `GITHUB_TOKEN` atau `GH_TOKEN`
- `APP_BASE_URL=http://localhost:8080`
- `OPENAPI_SPEC_PATH` default ke `C:\Software\gayakini\docs\brand-fashion-ecommerce-api-final.yaml`
- `TOOLS_MODE=dynamic` untuk launcher HTTP kecuali ada kebutuhan eksplisit
- `BROWSER_BASE_URL=http://localhost:8080`
- `BROWSER_PREFERENCE=default`
- `BROWSER_EXECUTABLE_PATH`, `CHROME_PATH`, `CHROME_BIN` opsional untuk override browser executable
- `EDGE_PATH` / `MS_EDGE_PATH` bisa dipakai jika browser target adalah Edge
- `PUPPETEER_LAUNCH_OPTIONS` dan `ALLOW_DANGEROUS` hanya untuk kasus browser automation yang memang butuh override

## MCP Server Set
Tujuh server MCP lokal yang didukung:
1. `gayakini-filesystem`
2. `gayakini-postgres`
3. `gayakini-github`
4. `gayakini-git`
5. `gayakini-terminal`
6. `gayakini-http`
7. `gayakini-browser`

## Maintenance Workflow
1. Ubah helper bersama di `tooling\mcp\common.ps1` hanya bila ada manfaat lintas launcher yang jelas.
2. Setelah mengubah launcher, jalankan `-ValidateOnly` untuk launcher terkait, lalu seluruh set jika perubahan menyentuh helper/doc/workflow.
3. Sinkronkan `AGENTS.md`, overlay provider, dan dokumen MCP jika ada perubahan nama launcher, env vars, assumptions, atau flow verifikasi.
4. Jika `.vscode` atau workflow berubah, pastikan task/CI tetap cocok dengan launcher pattern Windows-first.

## Troubleshooting Workflow
1. Jika launcher gagal, cek `npx.cmd` resolution dan env vars yang dipakai launcher.
2. Untuk `github`, validasi token source tanpa pernah menulis token ke repo.
3. Untuk `postgres`, cek dulu resolved target dan konektivitas lokal sebelum asumsi schema/credential salah.
4. Untuk `http`, cek `APP_BASE_URL` dan `OPENAPI_SPEC_PATH`; gunakan spec lokal sebelum mencoba spec runtime.
5. Untuk `browser`, tidak ada native browser-type flag. Edge didukung lewat `executablePath` ke `msedge.exe`; jika tidak tersedia, fallback adalah package default Chromium/Puppeteer behavior.

## Release / Hardening Workflow
1. Audit diff launcher/helper/doc/workflow/workspace sebagai satu paket kecil dan reviewable.
2. Jalankan preflight 7 launcher.
3. Jalankan verifikasi workspace/doc parity yang relevan.
4. Jangan commit/push sebelum hasil verifikasi aktual tersedia.
