# Contributing to Gayakini Backend

Terima kasih telah berkontribusi ke Gayakini! Proyek ini menggunakan arsitektur modular monolith dengan Spring Boot dan Kotlin. Berikut adalah panduan untuk menjaga kualitas dan konsistensi kode.

## Kode Etik Pengembangan

1.  **Contract-First (OpenAPI)**: Selalu perbarui `docs/brand-fashion-ecommerce-api-final.yaml` sebelum mengubah endpoint di controller. Gunakan file ini sebagai *source of truth*.
2.  **Modular Monolith**: Jaga agar dependensi antar modul tetap bersih. Gunakan `common` untuk utilitas yang benar-benar global.
3.  **Local-First Stability**: Pastikan aplikasi selalu bisa dijalankan secara lokal dengan `localSetup` dan `bootRun`.

## Workflow Kontribusi

### 1. Persiapan Lingkungan
Pastikan Anda sudah menjalankan `./gradlew localSetup` dan semua prasyarat terpenuhi. Gunakan `./gradlew doctor` untuk verifikasi.

### 2. Standar Kualitas Kode (Linting & Formatting)
Kami menggunakan **KtLint** dan **Detekt**. Keduanya bersifat **blocking** di CI.
- Jalankan `./gradlew ktlintCheck` untuk mengecek format.
- Jalankan `./gradlew ktlintFormat` untuk memperbaiki format otomatis.
- Jalankan `./gradlew detekt` untuk analisis statis.

### 3. Testing
Setiap fitur baru atau perubahan harus disertai dengan unit test atau integration test.
- Unit Test: Fokus pada domain logic.
- Integration Test: Fokus pada persistensi (Flyway/PostgreSQL) dan interaksi antar modul.
- Smoke Test: Gunakan `.http` files di folder `http/` untuk verifikasi cepat.

### 4. Git Flow
- Buat branch dari `main` dengan format: `feat/nama-fitur`, `fix/nama-bug`, atau `refactor/nama-modul`.
- Selalu jalankan `./gradlew releaseCheck` secara lokal sebelum melakukan push.

## Release Gate (The Gold Standard)

Sebelum melakukan Pull Request atau push ke `main`, pastikan perintah berikut lulus tanpa error:

```bash
./gradlew releaseCheck
```

Task ini mencakup:
- `clean`: Membersihkan build sebelumnya.
- `doctor`: Verifikasi environment.
- `ktlintCheck`: Cek formatting.
- `detekt`: Analisis kualitas kode.
- `test`: Menjalankan seluruh test suite.
- `flywayValidateLocal`: Verifikasi integritas migrasi database.
- `validateMcp`: Verifikasi kestabilan launcher MCP.

## Dokumentasi
- Update `CHANGELOG.md` untuk setiap perubahan signifikan.
- Jika ada perubahan pada skema database, pastikan file `docs/ecommerce_erd.mmd` atau dokumen terkait diperbarui.
- Update `README.md` jika ada perubahan pada cara setup atau run aplikasi.

## Menghubungi Maintainer
Jika ada pertanyaan mengenai arsitektur atau keputusan teknis, silakan buka Issue atau diskusikan di channel koordinasi tim.
