# RTK Terminal Filter: Gayakini Adaption (V2)

Adaptasi arsitektur [rtk-ai/rtk](https://github.com/rtk-ai/rtk) untuk efisiensi token LLM di repository Gayakini.

## Overview
RTK (Reduced Terminal Kontext) bertindak sebagai proxy/filter untuk output terminal. Tujuannya adalah menyaring noise (progress bars, repetitive stack traces, banner, dll.) sehingga agent menerima informasi yang padat dan relevan saja.

## Komponen
1.  **`filter-terminal-output.ps1`**: Script inti pemrosesan teks menggunakan Regex.
2.  **`rtk-rules.json`**: Konfigurasi pola noise, pengelompokan, dan batas pemotongan.
3.  **`tee-output.ps1`**: Menyimpan output asli (raw) ke `logs/` untuk pemulihan bukti jika filter terlalu agresif.
4.  **`rtk.ps1`**: Wrapper/Proxy utama untuk menjalankan perintah.
5.  **`fixtures/`**: Sampel output noisy untuk pengujian regresi.
6.  **`replay-fixture.ps1`**: Helper untuk menguji filter terhadap file fixture.

## Cara Kerja
Saat `RTK_ENABLED=true`:
1.  Perintah dijalankan via `rtk.ps1`.
2.  Output mentah disimpan ke `tooling/rtk/logs/`.
3.  Output difilter menggunakan `filter-terminal-output.ps1`.
4.  Jika filtering gagal, sistem otomatis fallback ke output mentah (fail-safe).
5.  Exit code asli dari perintah selalu dipertahankan.

## Mode & Profile
-   **Modes**:
    -   `filtered`: (Default) Menampilkan output yang diringkas.
    -   `passthrough`: Menampilkan output asli tanpa filter.
    -   `summarize-on-error`: Ringkasan hanya ditampilkan jika command gagal.
-   **Profiles**:
    -   `conservative`: Hanya strip ANSI dan drop noise dasar.
    -   `balanced`: (Default) Stack trace collapsing + deduplikasi.
    -   `aggressive`: Ringkasan sangat ketat (cocok untuk perintah yang sangat noisy).

## Penggunaan & Benchmark
Agent disarankan memakai `rtk.ps1` untuk perintah panjang. Benchmark dapat diaktifkan untuk melihat efisiensi pengurangan noise.

```powershell
# Jalankan perintah dengan filter dan lihat penghematan token
.\tooling\rtk\rtk.ps1 -Cmd "./gradlew test" -Benchmark
```

Output benchmark akan menampilkan:
-   `RAW_LINES` vs `FILTERED_LINES`
-   `REDUCTION %` (Biasanya 70-90% untuk kegagalan test/build)
-   `EST_FILTERED_TOKENS` (Estimasi beban konteks AI)

## Observabilitas
Semua output mentah dapat ditemukan di `tooling/rtk/logs/raw-*.log`. Jika agent ragu dengan hasil filter, rujuk file tersebut.
