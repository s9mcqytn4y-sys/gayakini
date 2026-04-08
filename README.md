# Gayakini Backend API

Backend e-commerce berbasis Spring Boot 3.4 + Kotlin 2.0 dengan arsitektur modular monolith. Fokus repo saat ini adalah local-first retail core yang stabil untuk operasi single-outlet, dengan kontrak REST yang jujur dan asset QA yang bisa dipakai ulang.

## Quick Start Lokal

### 1. Prasyarat
- JDK 17
- PostgreSQL 18+ lokal
- Node.js 18+ (untuk MCP launcher)

### 2. Setup environment
```bash
./gradlew localSetup
```

Default lokal pada `.env.example`:
- `DB_HOST=localhost`
- `DB_PORT=5432`
- `DB_NAME=gayakini`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=password`
- `DB_SCHEMA=commerce`

### 3. Preflight lokal
```bash
./gradlew doctor
```

### 4. Run server
```bash
./gradlew bootRun
```

Endpoint utama:
- API base: `http://localhost:8080/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health: `http://localhost:8080/actuator/health`

## Workflow Gradle

| Command | Fungsi |
|---|---|
| `./gradlew clean` | Bersihkan output build |
| `./gradlew doctor` | Cek Java, `.env`, dan PostgreSQL lokal |
| `./gradlew test` | Jalankan suite unit/integration |
| `./gradlew build` | Build aplikasi |
| `./gradlew validateMcp` | Verifikasi 7 launcher MCP lokal |
| `./gradlew smokeTest` | Quick HTTP smoke terhadap server yang sedang running |
| `./gradlew releaseCheck` | **Release Gate**: doctor + ktlint + detekt + test + flywayValidateLocal + validateMcp |

**Catatan Quality Gate:** `ktlintCheck` dan `detekt` bersifat **blocking** dalam task `releaseCheck`. Pastikan semua issue diselesaikan atau dimasukkan ke baseline sebelum push.

## Struktur Repo

```text
src/main/kotlin/com/gayakini/
|- api              # endpoint platform/global
|- cart             # cart guest + customer
|- catalog          # public catalog + admin catalog
|- checkout         # checkout dan shipping selection
|- common           # response/error/idempotency/util
|- customer         # auth, me, addresses, roles
|- infrastructure   # config, security, HTTP clients
|- inventory        # stock reservation/release
|- location         # shipping area lookup
|- order            # order lifecycle + admin order ops
|- payment          # payment session + Midtrans processing
|- shipping         # shipping quote, shipment booking, tracking state
|- webhook          # webhook ingress
```

## MCP Servers (Local Automation)

Tersedia 7 server MCP untuk membantu pengembangan lokal:
1. `filesystem`: Akses file repository.
2. `postgres`: Query database lokal.
3. `github`: Query repository/PR/metadata.
4. `git`: Operasi git lokal.
5. `terminal`: Eksekusi gradle/command.
6. `http`: Interaksi API via OpenAPI spec.
7. `browser`: Browser automation via Puppeteer.

Launcher tersedia di `tooling/mcp/start-*.ps1`. Dokumentasi lengkap ada di [docs/tooling/mcp-servers.md](docs/tooling/mcp-servers.md).

## Testing dan HTTP Assets

Asset manual test ada di `http/` dan sudah memakai prefix canonical `/v1`.

File utama:
- `http/01-smoke.http`
- `http/10-auth.http`
- `http/20-catalog.http`
- `http/30-cart.http`
- `http/40-checkout.http`
- `http/50-order.http`
- `http/90-webhooks.http`
- `http/80-admin.http`

Dokumen pendukung:
- [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md)
- [docs/TESTING.md](docs/TESTING.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)

## Midtrans Sandbox Setup

Untuk mengaktifkan integrasi Midtrans Sandbox secara lokal:

1.  **Konfigurasi `.env`**: Salin `.env.example` ke `.env` dan isi variabel berikut dengan key dari [Midtrans Dashboard](https://dashboard.sandbox.midtrans.com/):
    ```env
    MIDTRANS_SERVER_KEY=SB-Mid-server-xxxxxxxxxxxx
    MIDTRANS_CLIENT_KEY=SB-Mid-client-xxxxxxxxxxxx
    ```
2.  **Aktifkan Profile**: Jalankan aplikasi dengan profile `sandbox`. Profile ini akan menggabungkan konfigurasi `local` dan `sandbox`.
    ```bash
    java -jar build/libs/gayakini-0.0.1-SNAPSHOT.jar --spring.profiles.active=sandbox
    # Atau via gradle
    ./gradlew bootRun --args='--spring.profiles.active=sandbox'
    ```
3.  **Fail-Fast Security**: Aplikasi akan **gagal start** jika:
    - `MIDTRANS_IS_PRODUCTION` diset ke `true` saat menggunakan profile `sandbox` atau `local`.
    - URL Midtrans tidak mengandung kata `sandbox`.
    - Key Midtrans masih menggunakan nilai dummy ("dummy-server-key").

Lihat [docs/adr/0001-sandbox-first-midtrans-strategy.md](docs/adr/0001-sandbox-first-midtrans-strategy.md) untuk detail strategi arsitektur.
