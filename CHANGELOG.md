# Changelog Gayakini Backend

Format changelog ini mengikuti [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) dan menggunakan [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `CONTRIBUTING.md` untuk panduan kontribusi developer.
- `CHANGELOG.md` untuk pelacakan perubahan proyek.
- Dokumentasi `LOCAL_DEVELOPMENT.md` yang diperbarui.
- Integrasi Midtrans Sandbox dengan `fail-fast security` checks.
- Task `./gradlew localSetup` untuk mempermudah setup awal lingkungan lokal.
- Launcher MCP (Model Context Protocol) untuk otomatisasi lokal (filesystem, postgres, http, terminal, git, github, browser).

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
*Last update: 2025-05-24*
