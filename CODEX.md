# Codex Overlay: gayakini

`AGENTS.md` adalah source of truth operasional repo ini. `CODEX.md` adalah overlay Codex yang menambahkan penekanan workflow, bukan aturan yang bersaing.

## Codex Operating Notes
- Gunakan launcher lokal dengan pola:
  `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-*.ps1`
- Jalankan `-ValidateOnly` sebelum menganggap launcher siap dipasang ke workspace atau client MCP.
- Untuk launcher HTTP, default aman adalah spec lokal repo + base URL lokal.
- Untuk launcher browser, treat as local automation helper; target URL lokal default adalah `http://localhost:8080`.

## MCP Server Intent
- `gayakini-filesystem`: baca repo dan file kerja lokal
- `gayakini-git`: status/log/diff repo lokal
- `gayakini-terminal`: gradle dan shell verification
- `gayakini-postgres`: schema/query database lokal
- `gayakini-github`: query repo/PR/workflow
- `gayakini-http`: OpenAPI-driven local API helper
- `gayakini-browser`: browser automation via Puppeteer

## Edge Guidance
- Tidak ada native `browserType=edge` flag di launcher/browser package saat ini.
- Edge didukung lewat executable path ke `msedge.exe` bila tersedia.
- Jika Edge tidak tersedia atau tidak dikonfigurasi, fallback adalah executable default yang dipilih Puppeteer/package.

## Doc Sync Rule
Jika mengubah salah satu dari berikut:
- `tooling/mcp/**`
- `.vscode/**`
- `.github/workflows/**`
- `docs/agents/**`
- `docs/tooling/**`

maka sinkronkan juga:
- `AGENTS.md`
- `gemini.md`
- `CLAUDE.md`
- `CODEX.md`

## Verification Workflow
1. Jalankan `-ValidateOnly` untuk semua launcher.
2. Jalankan task/parity check workspace yang relevan.
3. Pastikan overlay provider tetap konsisten dengan `AGENTS.md`.

## Maintenance Workflow
1. Treat `AGENTS.md` as the repo-wide operational contract.
2. Keep `CODEX.md` additive and concise.
3. Update `docs/agents/codex.md` dan `docs/agents/codex-mcp-prompts.md` bila flow operasi Codex berubah.

## Troubleshooting Workflow
1. Cek helper/common assumptions dulu sebelum mengubah launcher individual.
2. Untuk browser, jangan asumsi Edge native mode ada; gunakan executable path atau fallback default.
3. Untuk HTTP, pastikan masalah bukan sekadar spec/base URL mismatch.

## Release / Hardening Workflow
1. Review intended files only.
2. Re-run launcher preflight + doc parity.
3. Commit/push hanya setelah evidence verifikasi tersedia.
