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
|- promo            # promotion engine (fixed/percentage)
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
- `http/90-webhooks.http` (Midtrans & Biteship)
- `http/80-admin.http`

## Webhook Security & Testing

Gayakini menggunakan strategi **Authoritative Reconciliation** untuk semua webhook:
1. **Strict Signature Validation**: Setiap payload divalidasi menggunakan SHA512 HMAC (Midtrans) atau Secret Header (Biteship).
2. **Anti-Spoofing Reconciliation**: Sistem tidak pernah mempercayai payload webhook secara membabi buta. Setelah signature valid, sistem akan melakukan *direct call* ke API provider (Misal: `[GET] /v2/{order_id}/status`) untuk mendapatkan status resmi.
3. **Audit Trail & Event-Driven Logging**: Setiap perubahan status bisnis (Order, Payment, Promo) dicatat secara terpusat di tabel `audit_logs` menggunakan Spring `ApplicationEventPublisher`. 
    - **Non-blocking**: Pencatatan audit dilakukan via `@TransactionalEventListener` sebelum commit.
    - **Traceability**: Mencatat `actor_id`, `actor_role`, `previous_state`, dan `new_state` (JSONB).
    - **Security**: Data sensitif (password, token, signature) disensor secara otomatis.
    - **Admin Access**: Audit trail dapat diakses oleh Admin melalui `GET /api/v1/admin/audits`.
4. **Anti-Spoofing Reconciliation**: Sistem tidak pernah mempercayai payload webhook secara membabi buta. Setelah signature valid, sistem akan melakukan *direct call* ke API provider (Misal: `[GET] /v2/{order_id}/status`) untuk mendapatkan status resmi.

### Testing Webhook Lokal
Untuk mengetes webhook dari provider luar (Midtrans/Biteship) ke mesin lokal Anda:
1. Gunakan **ngrok** atau **localtunnel**: `ngrok http 8080`.
2. Update URL webhook di Dashboard Provider ke URL ngrok Anda (Misal: `https://abcd-123.ngrok-free.app/v1/webhooks/midtrans`).
3. Gunakan file `http/90-webhooks.http` untuk simulasi payload tanpa ngrok.

## Billing & Document Generation

Gayakini menyediakan sistem pembuatan E-Receipt/Invoice otomatis yang aman dan skalabel:
1. **Asynchronous Execution**: Invoice di-render secara background setelah status pembayaran menjadi `PAID`. Proses ini tidak menghambat respon webhook utama.
2. **Professional PDF Rendering**: Menggunakan **Thymeleaf** (templating) dan **OpenHTMLtoPDF** dengan dukungan lokalisasi Indonesia (IDR, Tanggal).
3. **Secure Storage Architecture**:
    - **Directory Chunking**: File disimpan dengan struktur `YYYY/MM/DD` untuk performa filesystem yang optimal.
    - **Secure Naming**: Nama file menggunakan generator kriptografis (`INV-{ORDER_NUMBER}-{HASH}.pdf`) untuk mencegah *ID enumeration*.
4. **RBAC Streaming API**: Invoice dapat di-download melalui `GET /v1/orders/{orderId}/invoice`. Akses dibatasi ketat hanya untuk pemilik pesanan atau `ROLE_ADMIN`.
5. **Audit Integration**: Setiap keberhasilan atau kegagalan pembuatan dokumen dicatat dalam Audit Trail sistem.

Dokumen pendukung:
- [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md)
- [docs/TESTING.md](docs/TESTING.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/security-rbac.md](docs/security-rbac.md)

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
