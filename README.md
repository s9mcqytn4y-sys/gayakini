# Gayakini API

Backend e-commerce RESTful API berbasis Spring Boot 3.4+ (Java 17 & Kotlin) dengan arsitektur Modular Monolith.

## Operational Readiness: Status MVP
Proyek ini telah melalui fase Hardening & Verification dengan fitur utama:
- **Auth Identity Wiring:** Penggunaan `UserPrincipal` untuk mengikat Order ke Customer atau Guest Token secara riil.
- **Pricing Integrity:** Kalkulasi harga pesanan otomatis berdasarkan data database.
- **Webhook Processing:** Handler riil untuk Midtrans dan Biteship dengan verifikasi signature.
- **Idempotency:** Perlindungan terhadap duplikasi pesanan dan pengolahan webhook ganda.

## Tech Stack
- **Language:** Kotlin 1.9.25 (JDK 17)
- **Framework:** Spring Boot 3.4.0
- **Database:** PostgreSQL 18+ (UUIDv7)
- **Migration:** Flyway
- **Security:** Spring Security (Stateless, CORS Configured)
- **Integrations:** Midtrans Snap (Payments), Biteship (Shipping)

## Cara Menjalankan di Lokal

### 1. Setup Environment
Salin `.env.example` menjadi `.env` (atau `local.env` jika menggunakan VS Code):
```bash
cp .env.example .env
```
**PENTING:** Jangan pernah melakukan commit pada file `.env` atau `local.env`. File-file ini sudah masuk dalam `.gitignore`.

### 2. Jalankan Database
```bash
docker run --name gayakini-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=gayakini -p 5432:5432 -d postgres:18-alpine
```

### 3. Build & Run
```bash
./gradlew bootRun
```

## Verifikasi & Quality Gate
Lakukan urutan ini sebelum melakukan push:
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew test`
5. `./gradlew build`

## Dokumentasi API
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Keamanan
- **Secrets Management:** Gunakan environment variables. Jangan hardcode API Keys.
- **Rotation:** Jika secara tidak sengaja melakukan push rahasia ke repository, segera rotate key tersebut di provider terkait (Midtrans/Biteship).
