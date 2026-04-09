# RTK Terminal Filter: Gayakini Adaption

Adaptasi arsitektur [rtk-ai/rtk](https://github.com/rtk-ai/rtk) untuk efisiensi token LLM di repository Gayakini.

## Overview
RTK (Reduced Terminal Kontext) bertindak sebagai proxy/filter untuk output terminal. Tujuannya adalah menyaring noise (progress bars, repetitive stack traces, banner, dll.) sehingga agent (Gemini, Codex, Claude) menerima informasi yang padat dan relevan saja.

## Komponen
1.  **`filter-terminal-output.ps1`**: Script inti pemrosesan teks. Menggunakan Regex dan Rules untuk memadatkan output.
2.  **`rtk-rules.json`**: Konfigurasi pola noise, pengelompokan, dan batas pemotongan teks.
3.  **`tee-output.ps1`**: Menyimpan output asli (raw) ke disk untuk keperluan recovery/debugging jika filter terlalu agresif.
4.  **`rtk.ps1`**: Wrapper utama untuk menjalankan command (Proxy).

## Cara Kerja
Saat `RTK_ENABLED=true` di set di environment:
1.  Command dijalankan dan output ditangkap.
2.  Raw output disimpan ke `tooling/rtk/logs/`.
3.  Output difilter berdasarkan profile yang dipilih (`balanced` secara default).
4.  Agent menerima output yang sudah diringkas beserta referensi file log asli jika terjadi failure.

## Konfigurasi (Environment Variables)
-   `RTK_ENABLED`: `true` atau `false`.
-   `RTK_PROFILE`: `conservative`, `balanced`, atau `aggressive`.
-   `RTK_MAX_LINES`: Batas jumlah baris output (default: 100).
-   `RTK_MAX_CHARS`: Batas jumlah karakter output (default: 10000).

## Penggunaan oleh Agent
Agent disarankan untuk menjalankan perintah melalui wrapper RTK jika output diperkirakan akan sangat panjang atau noisy (seperti `./gradlew test` atau build yang gagal).

Contoh:
```powershell
.\tooling\rtk\rtk.ps1 -Cmd "./gradlew test"
```

## Fallback
Jika filter gagal atau ada kesalahan konfigurasi, sistem akan otomatis melakukan passthrough (menampilkan output asli) agar tidak mengganggu workflow.
