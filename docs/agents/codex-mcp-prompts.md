# Codex MCP Prompts

Prompt snippets ini dipakai untuk menjaga operasi Codex tetap aman dan konsisten di setup MCP lokal gayakini.

`AGENTS.md` adalah source of truth operasional. `CODEX.md` dan dokumen ini adalah overlay Codex.

## Default Orientation

Gunakan server sesuai fungsi:

- `filesystem-gayakini` untuk baca file repo
- `git-gayakini` untuk status, log, diff repo lokal
- `terminal-gayakini` untuk gradle dan command verifikasi
- `postgres-gayakini` untuk schema/query database lokal
- `github-gayakini` untuk query repo/PR/workflow
- `http-gayakini-local` untuk eksplor API lokal dari OpenAPI
- `browser-gayakini` untuk browser automation lokal

## Prompt Patterns

### Audit repository context

```text
Gunakan filesystem dan git MCP untuk memahami struktur repo, file kunci, dan perubahan aktif sebelum mengusulkan perubahan.
```

### Verify local backend

```text
Gunakan terminal MCP untuk menjalankan gradlew clean, ktlintCheck, detekt, test, dan build. Laporkan hasil aktual, jangan simpulkan tanpa output.
```

### Inspect local database

```text
Gunakan postgres MCP hanya untuk query schema/data lokal yang relevan. Hindari operasi destruktif tanpa konfirmasi eksplisit.
```

### Explore local API safely

```text
Gunakan HTTP MCP mode dynamic untuk melihat endpoint yang tersedia, baca schema endpoint yang relevan, lalu panggil endpoint lokal seperlunya. Default base URL adalah http://localhost:8080.
```

### Browser-assisted validation

```text
Gunakan browser MCP hanya bila butuh validasi flow UI/browser nyata. Mulai dari navigate, inspect, dan screenshot; jangan pakai launch options berbahaya kecuali dibutuhkan. Jika perlu Edge, gunakan executable path ke msedge.exe karena tidak ada browser-type flag native.
```

### GitHub review mode

```text
Gunakan GitHub MCP untuk baca PR, issue, workflow, dan metadata repo. Asumsikan mode read/query oriented kecuali user secara eksplisit meminta publish/write action.
```

## Guardrails

- Mulai dari `-ValidateOnly` untuk semua launcher baru atau yang baru diubah.
- Jangan mengarang keberadaan endpoint, schema, atau tool MCP.
- Untuk HTTP MCP, lebih aman pakai spec lokal repo daripada bergantung pada endpoint docs yang mungkin belum hidup.
- Untuk browser MCP, prefer local desktop automation; jangan anggap environment CI/headless tersedia.
- Jika launcher/helper/workflow berubah, sinkronkan `AGENTS.md`, `CODEX.md`, dan dokumen MCP terkait pada turn yang sama.
