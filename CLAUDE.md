# Claude Overlay: gayakini

`AGENTS.md` adalah source of truth operasional repo ini. `CLAUDE.md` hanya overlay provider-specific untuk Claude dan tidak boleh bertentangan dengannya.

## Focus
- Gunakan launcher MCP lokal berbasis PowerShell Windows.
- Mulai dari `-ValidateOnly` sebelum wiring launcher ke konfigurasi client.
- Untuk GitHub, pertahankan mode read/query oriented kecuali user meminta aksi write secara eksplisit.

## MCP Set
- `gayakini-filesystem`
- `gayakini-postgres`
- `gayakini-github`
- `gayakini-git`
- `gayakini-terminal`
- `gayakini-http`
- `gayakini-browser`

## Provider Notes
- HTTP launcher default ke `http://localhost:8080` + spec lokal repo.
- Browser launcher memakai Puppeteer MCP server. Edge tidak punya browser-type flag native; gunakan path `msedge.exe` via `BROWSER_EXECUTABLE_PATH`, `EDGE_PATH`, atau `MS_EDGE_PATH`.
- Jangan simpan token atau env sensitif ke file repo.

## Antigravity IDE Workflows
Equivalent workflows tersedia di `.agents/workflows/`:
- `/validate-mcp-launchers` — MCP launcher preflight
- `/docs-parity-check` — Doc parity check
- `/mcp-hardening-preflight` — Full MCP hardening
- `/gradle-release-verification` — Full Gradle quality gate
- `/run-application` — Jalankan server lokal

## Verification
Sebelum menyimpulkan setup MCP siap:
1. Jalankan `-ValidateOnly` untuk semua `tooling\mcp\start-*.ps1`.
2. Sinkronkan perubahan launcher ke `AGENTS.md`, `gemini.md`, `CLAUDE.md`, `CODEX.md`, dan dokumen di `docs/`.
3. Pastikan task `.vscode`, `.agents/workflows/`, dan workflow `.github` masih sejalan dengan flow launcher.

## Maintenance Workflow
1. Perlakukan `AGENTS.md` sebagai sumber aturan utama.
2. Jika launcher/helper berubah, sinkronkan overlay provider dan dokumen MCP teknis pada turn yang sama.
3. Pertahankan workspace/task validation tetap sejalan dengan launcher pattern Windows-first.

## Troubleshooting Workflow
1. Mulai dari `-ValidateOnly`.
2. Untuk HTTP, cek `APP_BASE_URL` dan spec lokal.
3. Untuk browser, cek executable path; Edge hanya tersedia lewat path `msedge.exe`.
4. Untuk GitHub, validasi source token tanpa menulis secret ke repo.

## Release / Hardening Workflow
1. Review diff launcher, docs, workflow, dan workspace sebagai satu paket.
2. Jalankan preflight tujuh launcher.
3. Commit hanya file yang memang masuk scope hardening MCP/workspace/docs.
