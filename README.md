# gayakini API

Backend e-commerce RESTful API berbasis Spring Boot 3.4+ (Java 17 & Kotlin) dengan arsitektur Modular Monolith.

## Tech Stack
- **Language:** Kotlin 1.9.25 (JDK 17)
- **Framework:** Spring Boot 3.4.0
- **Database:** PostgreSQL 18+
- **Migration:** Flyway
- **Documentation:** OpenAPI 3 / Swagger
- **Security:** Spring Security (JWT ready)
- **Integrations:** Midtrans (Payments), Biteship (Shipping)
- **Quality Gates:** ktlint, detekt, Testcontainers

## Arsitektur: Modular Monolith
Proyek ini menggunakan struktur modular untuk memudahkan navigasi dan skalabilitas:
- `common`: Kode utilitas, base classes, dan konstanta global.
- `auth`: Autentikasi dan otorisasi.
- `catalog`: Manajemen produk, kategori, dan stok.
- `customer`: Profil pengguna dan alamat.
- `cart`: Pengelolaan keranjang (guest & auth).
- `checkout`: Proses kalkulasi biaya dan validasi sebelum order.
- `order`: Manajemen pesanan dan history.
- `payment`: Integrasi Midtrans dan status pembayaran.
- `shipping`: Integrasi Biteship dan tracking pengiriman.
- `infrastructure`: Konfigurasi teknis (Database, Security, Web).

Setiap modul dibagi menjadi lapisan:
1. `domain`: Entity, Repository Interface, Business Rules.
2. `application`: Use Cases / Services.
3. `infrastructure`: Repository Impl, External Adapters.
4. `api`: Controllers, DTOs, Mappers.

## Cara Menjalankan di Lokal

### 1. Prasyarat
- JDK 17
- Docker (untuk PostgreSQL via Testcontainers atau lokal)

### 2. Setup Environment
Salin `.env.example` menjadi `.env` dan sesuaikan nilainya:
```bash
cp .env.example .env
```

### 3. Menjalankan Database
Jika menggunakan Docker:
```bash
docker run --name gayakini-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=gayakini -p 5432:5432 -d postgres:18-alpine
```

### 4. Menjalankan Aplikasi
Gunakan Gradle wrapper:
```bash
./gradlew bootRun
```

### 5. Akses Dokumentasi
Setelah aplikasi jalan, buka:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Docs:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Testing & Quality Gate
- **Run Tests:** `./gradlew test`
- **Run Lint:** `./gradlew ktlintCheck`
- **Run Detekt:** `./gradlew detekt`

## Integrasi MCP (Cursor/Codex)
Proyek ini menyertakan konfigurasi MCP untuk mempercepat DX:
- Konfigurasi ada di `.cursor/mcp.json`.
- Pastikan Anda sudah menginstal MCP server yang relevan jika ingin menggunakan fitur repo/docs integration.

## Aturan Bisnis Penting
- **Uang:** Disimpan sebagai `bigint` (IDR).
- **ID:** Menggunakan UUIDv7.
- **Stock:** Menggunakan explicit lock (`SELECT ... FOR UPDATE`).
- **Idempotency:** Wajib di implementasikan pada flow Place Order dan Payment Webhooks.
- **Snapshot:** Tabel Order menyimpan snapshot data Produk dan Alamat saat transaksi terjadi.
