# gayakini API

Backend e-commerce RESTful API berbasis Spring Boot 3.4+ (Java 17 & Kotlin) dengan arsitektur Modular Monolith.

## Operational Readiness: Status MVP
Proyek ini telah melalui fase Hardening & Verification dengan fitur utama:
- **Auth Identity Wiring:** Penggunaan `UserPrincipal` untuk mengikat Order ke Customer atau Guest Token secara riil.
- **Pricing Integrity:** Kalkulasi harga pesanan otomatis berdasarkan data database, bukan nilai dummy.
- **Webhook Processing:** Handler riil untuk Midtrans dan Biteship dengan verifikasi signature dan pembaruan status pesanan.
- **Idempotency:** Perlindungan terhadap duplikasi pesanan dan pengolahan webhook ganda.

## Tech Stack
- **Language:** Kotlin 1.9.25 (JDK 17)
- **Framework:** Spring Boot 3.4.0
- **Database:** PostgreSQL 18+ (UUIDv7, Snapshot Posture)
- **Migration:** Flyway (V1: Initial, V2: Audit & Idempotency)
- **Security:** Spring Security (Stateless, CORS Configured)
- **Integrations:** Midtrans Snap (Payments), Biteship (Shipping)

## Cara Menjalankan di Lokal

### 1. Setup Environment
Salin `.env.example` menjadi `.env` dan isi kredensial pihak ketiga:
```bash
cp .env.example .env
```

### 2. Jalankan Database
Gunakan Docker untuk PostgreSQL 18+:
```bash
docker run --name gayakini-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=gayakini -p 5432:5432 -d postgres:18-alpine
```

### 3. Build & Run
```bash
./gradlew bootRun
```

## Verifikasi & Quality Gate
Pastikan semua verifikasi berikut dijalankan secara berurutan:
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew test`
5. `./gradlew build`

## Dokumentasi API
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Endpoint Utama (v1)
- `GET /api/v1/hello`: Health check & Greeting.
- `POST /api/v1/orders/place`: Membuat pesanan (Auth/Guest).
- `POST /api/v1/webhooks/midtrans`: Callback status pembayaran.
- `POST /api/v1/webhooks/biteship`: Callback status pengiriman.

## Aturan Bisnis & Keamanan
- **Money:** Selalu `Long` (IDR).
- **Stock:** `SELECT FOR UPDATE` saat reservasi barang.
- **Signature:** Webhook Midtrans divalidasi dengan Signature Key SHA-512.
- **Identity:** Menggunakan `SecurityUtils` untuk ekstraksi `UserPrincipal`.
