# Release Checklist: gayakini Backend

Gunakan checklist ini sebelum melakukan push ke `main` atau melakukan deployment. Checklist ini memastikan repository berada dalam kondisi "Minimum Viable Release".

## 1. Environment & Pre-flight
- [x] `.env` sudah di-setup (Gunakan `./gradlew localSetup`).
- [x] PostgreSQL lokal berjalan dan schema valid (Gunakan `./gradlew dbDoctor`).
- [x] Java 17 terdeteksi dengan benar.

## 2. Quality Gates (Blocking)
Jalankan task utama verifikasi:
```bash
./gradlew releaseCheck
```
Pastikan semua gate berikut lulus:
- [x] **Doctor**: Konektivitas DB dan environment OK (`./gradlew dbDoctor`).
- [x] **Lint (ktlint)**: Tidak ada pelanggaran style code.
- [x] **Static Analysis (detekt)**: Tidak ada issue kritikal atau code smell berat di luar baseline.
- [x] **Tests**: Semua 21 unit dan integration test lulus.
- [x] **Flyway Validation**: Schema lokal cocok dengan file migrasi (`./gradlew flywayValidateLocal`).
- [x] **MCP Validation**: Semua 7 launcher MCP lokal lulus `-ValidateOnly`.

## 3. Build & Container Readiness
- [x] Artifact `bootJar` berhasil dibuat (`./gradlew bootJar`).
- [ ] Docker image berhasil di-build tanpa error (`docker build -t gayakini-backend .`).
- [ ] Docker Compose bisa start dengan `postgres` + `app` (`docker-compose up -d`).
- [ ] Health check container `app` menunjukkan status healthy (cek `docker ps`).

## 4. Smoke Test (Runtime)
Dengan aplikasi yang sedang berjalan (lokal atau docker):
- [ ] Jalankan `./gradlew smokeTest`.
- [ ] Verifikasi endpoint `/actuator/health` mengembalikan `UP`.
- [ ] Verifikasi endpoint `/v1/products` mengembalikan data catalog.

## 5. Documentation Parity
- [x] `README.md` sinkron dengan workflow terbaru.
- [x] `AGENTS.md` sinkron dengan launcher dan task terbaru.
- [x] Overlay provider (`gemini.md`, `CLAUDE.md`, `CODEX.md`) sinkron.
- [x] OpenAPI spec (`docs/brand-fashion-ecommerce-api-final.yaml`) mencerminkan state code saat ini.

## 6. Git Hygiene
- [x] Tidak ada secret/token yang tidak sengaja ter-commit.
- [x] `.gitignore` sudah mencakup file sensitif dan output build.
- [x] Commit message jelas dan deskriptif.

---
**Status Compliance:** Jika semua poin di atas [x], repository dianggap **Deploy Standard Compliant**.
