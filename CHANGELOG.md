# Changelog Gayakini Backend

Format changelog ini mengikuti [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) dan menggunakan [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `gradle/scripts/local-env.gradle.kts` untuk manajemen lingkungan lokal yang terisolasi.
- `gradle/scripts/quality.gradle.kts` untuk orkestrasi quality gate (Ktlint, Detekt).
- Task `releaseCheck` sebagai authoritative quality gate tunggal (Clean + Quality + Assemble + MCP Validation).
- Dukungan penuh Gradle Configuration Cache pada task custom (`validateMcp`, `dbStart`).

### Changed
- Refaktor `build.gradle.kts` menjadi lebih modular dengan script plugins.
- Peningkatan batas baris Ktlint menjadi 120 karakter untuk konsistensi pembacaan database URL.
- Stabilisasi lifecycle commerce (Order, Product, Payment, Shipping) dengan hardening pada `OrderService`.
- Pembersihan total lingkungan testing (`src/test`) untuk mendukung model verifikasi berbasis task dan quality gate.

### Fixed
- Serialization error pada Gradle Configuration Cache untuk task yang menggunakan `ExecOperations` atau `Project` object di execution phase.
- Duplikasi logika loading `.env` dengan memusatkan state pada extra property `localEnv`.
- Inkonsistensi dokumentasi `AGENTS.md` terkait workflow testing yang sudah dihapus.

### Security
- Isolasi environment variable dalam task execution menggunakan `doFirst` dan `environment` injection.
- Hardening MCP launchers dengan validasi `-ValidateOnly` terotomatisasi.

### Changed
- Migrasi dari Spring Boot 3.3 ke Spring Boot 3.4.
- Update Kotlin ke versi 2.0.
- Perbaikan path matching pada `SecurityConfig` untuk endpoint `/v1/orders/**`.
- Optimalisasi `PaymentMethodIntegrationTest` dengan `RestTemplateConfig`.
- Peningkatan kualitas kode dengan penerapan ketat KtLint dan Detekt.
- Pemisahan HTTP assets ke folder `http/` dengan prefix canonical `/v1`.

### Fixed
- Error `UnsatisfiedDependencyException` pada test suite payment.
- Masalah akses guest (401 Unauthorized) pada endpoint detail order publik.
- Puluhan KtLint formatting violations di seluruh source code.
- Konsistensi penamaan endpoint pada OpenAPI spec `docs/brand-fashion-ecommerce-api-final.yaml`.

### Security
- Menambahkan pemeriksaan `MIDTRANS_IS_PRODUCTION` untuk mencegah penggunaan key sandbox di production.
- Pengaturan `SecurityConfig` yang lebih granular untuk public vs authenticated endpoints.
- Verifikasi schema Flyway dalam task `releaseCheck`.

---
*Last update: 2025-05-25*
