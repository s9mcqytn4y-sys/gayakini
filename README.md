# Gayakini Backend API

Backend e-commerce berbasis Spring Boot 3.4 + Kotlin 2.0 dengan arsitektur modular monolith. Fokus repo saat ini adalah local-first retail core yang stabil untuk operasi single-outlet, dengan kontrak REST yang jujur dan asset QA yang bisa dipakai ulang.

## Quick Start Lokal

### 1. Prasyarat
- JDK 17
- PostgreSQL 18+ lokal
- Node.js bila ingin memakai MCP launcher lokal

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
| `./gradlew test` | Jalankan suite unit/integration saat ini |
| `./gradlew build` | Build aplikasi |
| `./gradlew smokeTest` | Quick HTTP smoke terhadap server yang sedang running |
| `./gradlew releaseCheck` | Verifikasi sebelum push: doctor + ktlint + detekt + test + flywayValidateLocal |

Catatan: `ktlintCheck` dan `detekt` saat ini masih advisory karena repo masih punya debt historis yang belum diratchet menjadi strict gate.

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

## Testing dan HTTP Assets

Asset manual test ada di `http/` dan sudah memakai prefix canonical `/v1`.

File utama:
- `http/smoke.http`
- `http/auth.http`
- `http/catalog.http`
- `http/cart.http`
- `http/checkout.http`
- `http/order-flow.http`
- `http/webhooks.http`
- `http/80-admin-rbac.http`

Dokumen pendukung:
- [docs/LOCAL_DEVELOPMENT.md](docs/LOCAL_DEVELOPMENT.md)
- [docs/TESTING.md](docs/TESTING.md)
- [docs/FRONTEND_SANDBOX_INTEGRATION.md](docs/FRONTEND_SANDBOX_INTEGRATION.md)

## Observability dan Security Notes

- `GET /actuator/health` dan `GET /actuator/info` public.
- Endpoint actuator lain tidak dibuka untuk anonymous access.
- Error envelope aplikasi dibentuk oleh `GlobalExceptionHandler`; server-level message leakage dimatikan di config utama.
- Default Midtrans/Biteship tetap sandbox/local oriented. Jangan arahkan config default ke production.
