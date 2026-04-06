# Local Development Guide

## Prerequisites
- **JDK 17+** (Amazon Corretto or Temurin recommended)
- **PostgreSQL 18+** (Running locally, Windows/Scoop friendly)

## Initial Setup
1. Run the `localSetup` task to initialize your `.env` file:
   ```bash
   ./gradlew localSetup
   ```
2. Configure your `.env` file with your local database credentials if they differ from the defaults:
   - `DB_HOST=localhost`
   - `DB_PORT=5432`
   - `DB_NAME=gayakini`
   - `DB_USERNAME=postgres`
   - `DB_PASSWORD=password`
   - `DB_SCHEMA=commerce`
   - `MIDTRANS_API_URL=https://api.sandbox.midtrans.com/v2`
   - `MIDTRANS_SNAP_URL=https://app.sandbox.midtrans.com/snap/v1/transactions`
   - `BITESHIP_API_URL=https://api.biteship.com/v1`
   - `BITESHIP_WEBHOOK_SECRET=<local-secret>`

## Running the Application
Use the existing `bootRun` task. Task ini sudah menjalankan preflight `ensurePostgres` dan `flywayMigrateLocal` sebelum aplikasi start.

```bash
./gradlew bootRun
```

Jika PostgreSQL lokal belum menyala, `bootRun` akan mencoba menyalakan service lewat `pg_ctl` lebih dulu.

## Quality Gates
Before pushing any code, run the `releaseCheck` task:
```bash
./gradlew releaseCheck
```
This runs:
- `ktlintCheck`: Linting
- `detekt`: Static analysis
- `test`: Unit and Integration tests
- `flywayValidateLocal`: Flyway check

Catatan: `ktlintCheck` dan `detekt` saat ini masih advisory. Output-nya tetap harus direview walau build belum fail untuk seluruh temuan historis.

## Testing Endpoints
We provide a suite of `.http` files in the `http/` directory. These can be run directly from IntelliJ IDEA or VS Code (with REST Client extension).

1. Open `http/smoke.http`
2. Select the `local` environment from `http/env/local.http.env.json`
3. Execute the requests.

Urutan manual test yang disarankan:
1. `http/smoke.http`
2. `http/auth.http`
3. `http/catalog.http`
4. `http/cart.http`
5. `http/checkout.http`
6. `http/order-flow.http`
7. `http/webhooks.http`

## Database Migrations
To check the status of your migrations:
```bash
./gradlew flywayInfoLocal
```
To run migrations manually:
```bash
./gradlew flywayMigrateLocal
```
