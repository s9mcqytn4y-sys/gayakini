# Codex Agent Notes

Dokumen ini melengkapi `CODEX.md` dan `AGENTS.md`.

## Source Of Truth
- Operational truth: `AGENTS.md`
- Provider overlay: `CODEX.md`
- Prompt patterns: `docs/agents/codex-mcp-prompts.md`

## MCP Practical Use
- Gunakan `filesystem` + `git` untuk orientasi repo.
- Gunakan `terminal` untuk verifikasi yang butuh evidence aktual.
- Gunakan `http` untuk eksplor API lokal dari kontrak OpenAPI.
- Gunakan `browser` bila validasi interaksi browser benar-benar dibutuhkan.

## Hardening Expectations
- Jangan mengarang dukungan browser type yang tidak ada.
- Jangan menganggap API runtime hidup hanya karena spec lokal ada.
- Setelah perubahan launcher, jalankan preflight 7 server lalu cek parity dokumen.

## Troubleshooting Workflow
1. Mulai dari helper dan env resolution.
2. Lanjut ke `-ValidateOnly`.
3. Baru setelah itu cek runtime assumptions per server.
