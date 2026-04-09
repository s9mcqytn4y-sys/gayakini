# MCP Servers Local Setup

Dokumen ini adalah source of truth operasional untuk launcher MCP lokal di repo `C:\Software\gayakini`.

`AGENTS.md` tetap menjadi source of truth perilaku agent repo-wide. Dokumen ini adalah source of truth teknis untuk setup launcher MCP lokal.

## Tujuan

Setup ini disusun untuk Codex lokal di Windows dengan pola launcher:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-*.ps1
```

Semua launcher wajib punya mode `-ValidateOnly` dan wajib dicek dulu sebelum dipasang ke konfigurasi MCP client.

## Inventory

Launcher yang tersedia:

1. `tooling\mcp\start-filesystem.ps1`
2. `tooling\mcp\start-postgres.ps1`
3. `tooling\mcp\start-github.ps1`
4. `tooling\mcp\start-git.ps1`
5. `tooling\mcp\start-terminal.ps1`
6. `tooling\mcp\start-http.ps1`
7. `tooling\mcp\start-browser.ps1`

Shared helper:

- `tooling\mcp\common.ps1`

## Server Roles

### filesystem

- Scope: file access terbatas ke repo root.
- Default root: `C:\Software\gayakini`
- Override: `PROJECT_ROOT`, `REPO_ROOT`, atau parameter `-Root`

### postgres

- Scope: database lokal gayakini.
- Default:
  - host: `localhost`
  - port: `5432`
  - database: `gayakini`
  - user: `postgres`
  - password: `password`
- Override: `DATABASE_URL` atau env DB/PG biasa.

### github

- Scope: GitHub MCP untuk query/read repo.
- Token precedence:
  - `GITHUB_PERSONAL_ACCESS_TOKEN`
  - `GITHUB_TOKEN`
  - `GH_TOKEN`

### git

- Scope: operasi git terhadap repo lokal yang valid.
- Default repo: `C:\Software\gayakini`

### terminal

- Scope: command execution lokal untuk gradle/git/tooling.
- Dipakai untuk verifikasi evidence-driven, bukan shortcut klaim hasil.
- **RTK Filtering:** Mendukung pengurangan noise output via `tooling\rtk`.
  - `RTK_ENABLED=true`: Aktifkan filtering.
  - `RTK_PROFILE`: `conservative`, `balanced` (default), `aggressive`.
  - `RTK_MODE`: `filtered` (default), `passthrough`, `summarize-on-error`.
  - `RTK_TEE_DIR`: Folder untuk menyimpan raw log (default: `tooling\rtk\logs`).

### http

- Scope: expose API lokal sebagai MCP tools dari OpenAPI.
- Package: `@ivotoby/openapi-mcp-server`
- Default base URL: `http://localhost:8080`
- Default spec path: `docs\brand-fashion-ecommerce-api-final.yaml`
- Default tools mode: `dynamic`

Catatan:
- Launcher HTTP ini bukan package official dari namespace `@modelcontextprotocol/*`.
- Dipilih karena cocok untuk API lokal berbasis OpenAPI dan tidak memblokir `localhost`.

### browser

- Scope: browser automation lokal.
- Package: `@modelcontextprotocol/server-puppeteer`
- Default target URL untuk flow app lokal: `http://localhost:8080`
- Browser preference default: `default`
- Override optional:
  - `BROWSER_BASE_URL`
  - `BROWSER_PREFERENCE`
  - `BROWSER_EXECUTABLE_PATH`
  - `CHROME_PATH`
  - `CHROME_BIN`
  - `EDGE_PATH`
  - `MS_EDGE_PATH`
  - `PUPPETEER_LAUNCH_OPTIONS`
  - `ALLOW_DANGEROUS`

Catatan Edge:
- Tidak ada browser-type flag native.
- Jika ingin memaksa Edge, gunakan preference `edge` atau executable path `msedge.exe`.
- Jika Edge executable tidak ditemukan, launcher akan fail-fast bila preference explicit `edge` diminta.

## Validate Commands

Gunakan Gradle untuk validasi kolektif:
```bash
./gradlew validateMcp
```

Atau manual per server:
```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-postgres.ps1 -ValidateOnly
cmd /c "set GITHUB_PERSONAL_ACCESS_TOKEN=dummy_local_token&& powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-github.ps1 -ValidateOnly"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-git.ps1 -ValidateOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-terminal.ps1 -ValidateOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-http.ps1 -ValidateOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-browser.ps1 -ValidateOnly
```

## Antigravity IDE Workflows

Equivalent workflows untuk VSCode tasks tersedia di `.agents/workflows/`:

| Workflow | Equivalent VSCode Task |
|---|---|
| `/validate-mcp-launchers` | MCP: Validate launchers (via Gradle) |
| `/docs-parity-check` | MCP: Docs parity check |
| `/mcp-hardening-preflight` | MCP: Hardening preflight |
| `/gradle-release-verification` | Gradle: release verification (via releaseCheck) |
| `/run-application` | GayakiniApplication (launch config) |

## Safety Rules

- Jangan commit token atau local env.
- Jangan arahkan HTTP launcher ke production tanpa instruksi eksplisit.
- Jangan nyalakan `ALLOW_DANGEROUS=true` untuk browser kecuali memang perlu.
- Gunakan `TOOLS_MODE=dynamic` untuk HTTP sebagai default aman.
- GitHub MCP diposisikan query-first, bukan publish-first.

## Maintenance Workflow

1. Ubah `common.ps1` hanya untuk kebutuhan lintas launcher yang jelas.
2. Jika menambah env var, parameter, atau default baru, update launcher, `AGENTS.md`, overlay provider, dan dokumen ini bersama-sama.
3. Jika mengubah `.vscode`, `.agents/workflows/`, atau workflow, verifikasi task/CI masih cocok dengan launcher pattern Windows-first.

## Troubleshooting Workflow

1. Jalankan `./gradlew validateMcp`.
2. Jika gagal di `npx`, cek PATH atau `MCP_NPX`.
3. Jika `http` gagal, cek `APP_BASE_URL` dan `OPENAPI_SPEC_PATH`.
4. Jika `browser` gagal untuk Edge, cek `msedge.exe` path.
5. Jika `github` gagal, cek sumber token tanpa pernah mencetak atau menyimpan token ke repo.

## Release / Hardening Workflow

1. Review diff untuk launcher, docs, `.vscode`, dan workflow sebagai satu paket.
2. Jalankan `./gradlew releaseCheck`.
3. Jalankan validasi task/workflow/docs parity yang relevan.
4. Commit hanya file yang memang masuk scope hardening MCP/workspace/docs.
