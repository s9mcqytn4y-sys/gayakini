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
- **Preflight launcher:** Gunakan `-ValidateOnly` atau `./gradlew validateMcp` sebelum wiring ke Codex atau VS Code task.
- **GitHub MCP:** Gunakan read/query oriented workflow. Hindari write/publish kecuali user minta eksplisit dan sudah diverifikasi.
- **HTTP MCP:** Default ke OpenAPI lokal repo + base URL aplikasi lokal. Jangan arahkan ke production API.
- **Browser MCP:** Untuk automation lokal saja. Jangan pakai launch options berisiko kecuali perlu dan user paham dampaknya.
- **Doc sync:** Setiap perubahan di `tooling/mcp/**`, `.vscode/**`, `.agents/**`, atau workflow validasi wajib disinkronkan ke dokumen agent/provider terkait pada turn yang sama.

## Source Of Truth Model
- `AGENTS.md` adalah operational source of truth untuk perilaku agent lokal di repo ini.
- `gemini.md`, `CLAUDE.md`, dan `CODEX.md` adalah provider-specific overlays. Overlay boleh menambah penekanan provider, tapi tidak boleh bertentangan dengan `AGENTS.md`.
- Dokumen rinci MCP lokal ada di `docs/tooling/mcp-servers.md`.
- Checklist rilis ada di `docs/RELEASE_CHECKLIST.md`.

## Source of Truth Hierarchy
1. **Code Implementation** (Kotlin/Java)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Rules of Engagement
- **Architecture:** Modular Monolith. Tetap sederhana, hindari over-engineering.
- **Evidence-Based:** Jangan berasumsi build sukses tanpa menjalankan perintah Gradle.
- **Database:** Gunakan `UUIDv7`. Jangan edit file migrasi yang sudah rilis (Commit baru untuk perubahan).
- **Quality Gate:** Source code melewati Detekt dan Ktlint. Unit/integration testing telah dihapus untuk stabilitas siklus hidup commerce murni berbasis task.
- **Idempotency:** Wajib untuk Place Order, Payment, dan Webhook processing.

## Development Verification Flow
Gunakan `gayakini-terminal` untuk verifikasi:
1. `./gradlew clean`
2. `./gradlew qualityCheck` (Linting + Detekt)
3. `./gradlew build`
4. `./gradlew validateMcp` (Windows-only preflight)
5. `./gradlew releaseCheck` (Core quality gate: Clean + Quality + Assemble + MCP)
6. `./gradlew releaseCheckLocal` (Full gate: releaseCheck + Local DB validation)
7. `./gradlew dbDoctor` (Database & Environment diagnostics)

## RTK Terminal Filtering (Noise Reduction V2)
Repo ini mendukung filtering output terminal untuk efisiensi token LLM via adaptasi RTK.
- **Kapan Menggunakan:** Gunakan `.\tooling\rtk\rtk.ps1 -Cmd "..."` untuk perintah dengan output panjang atau repetitif (misal: `./gradlew test`).
- **Prinsip:** RTK membuang progress bar, deduplikasi log berulang, dan meringkas stack trace framework (seperti `org.gradle.*` atau `org.springframework.*`).
- **Benchmark:** Tambahkan `-Benchmark` untuk melihat penghematan token. Target reduksi: >70% untuk build noisy.
- **Fail-safe:** Jika filter gagal, output asli akan diteruskan. Raw logs selalu disimpan di `tooling/rtk/logs/`.
- **Integrasi:** Aktifkan global via `RTK_ENABLED=true` di `.env` (default: false).

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

## MCP Server Set
Tujuh server MCP lokal yang didukung:
1. `gayakini-filesystem`
2. `gayakini-postgres`
3. `gayakini-github`
4. `gayakini-git`
5. `gayakini-terminal`
6. `gayakini-http`
7. `gayakini-browser`

## Antigravity IDE Workflows
Equivalent `.agents/workflows/` untuk task VSCode di `.vscode/tasks.json`:
- `/validate-mcp-launchers` — Validate semua launcher `-ValidateOnly` via Gradle
- `/docs-parity-check` — Cek doc parity launcher terms
- `/mcp-hardening-preflight` — Combine validate + docs-parity + workspace JSON
- `/gradle-release-verification` — Full quality gate via `releaseCheck`
- `/run-application` — Jalankan aplikasi lokal via `bootRun`

## Maintenance Workflow
1. Ubah helper bersama di `tooling\mcp\common.ps1` hanya bila ada manfaat lintas launcher yang jelas.
2. Setelah mengubah launcher, jalankan `./gradlew validateMcp`.
3. Sinkronkan `AGENTS.md`, overlay provider, dan dokumen MCP jika ada perubahan nama launcher, env vars, assumptions, atau flow verifikasi.
4. Jika `.vscode`, `.agents`, atau workflow berubah, pastikan task/CI tetap cocok dengan launcher pattern Windows-first.

## Troubleshooting Workflow
1. Jika launcher gagal, cek `npx.cmd` resolution dan env vars yang dipakai launcher.
2. Untuk `github`, validasi token source tanpa pernah menulis token ke repo.
3. Untuk `postgres`, cek dulu resolved target dan konektivitas lokal sebelum asumsi schema/credential salah.
4. Untuk `http`, cek `APP_BASE_URL` and `OPENAPI_SPEC_PATH`.
5. Untuk `browser`, cek `msedge.exe` path jika menggunakan Edge.
6. Untuk Gradle stuck, gunakan `dbDoctor` untuk memastikan PostgreSQL tidak dalam state recovery yang menghalangi build.

## Release / Hardening Workflow
1. Audit diff launcher/helper/doc/workflow/workspace sebagai satu paket kecil dan reviewable.
2. Jalankan `releaseCheck`.
3. Jalankan verifikasi workspace/doc parity yang relevan.
4. Jangan commit/push sebelum hasil verifikasi aktual tersedia.
