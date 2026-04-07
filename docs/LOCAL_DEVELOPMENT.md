# Local Development Guide

## Prerequisites
- **JDK 17+** (Amazon Corretto or Temurin recommended)
- **PostgreSQL 18+** (Running locally, Windows/Scoop friendly)
- **Node.js 18+** (Required for MCP launchers)

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

## Running the Application
Use the existing `bootRun` task. This task automatically runs `ensurePostgres` and `flywayMigrateLocal` before the application starts.

```bash
./gradlew bootRun
```

If local PostgreSQL is not running, `bootRun` will attempt to start it via `pg_ctl`.

## Quality Gates (Mandatory)
Before pushing any code, you must run the `releaseCheck` task. This is the official release gate.
```bash
./gradlew releaseCheck
```
This runs:
- `doctor`: Environment and DB connectivity diagnostics.
- `ktlintCheck`: Code style enforcement (**Blocking**).
- `detekt`: Static analysis (**Blocking**).
- `test`: Unit and Integration tests (**Blocking**).
- `flywayValidateLocal`: Migration integrity check.
- `validateMcp`: MCP launcher preflight.

If any of these fail, the release is considered blocked.

## MCP Servers
The repository includes 7 local MCP servers for development automation.
- Run `./gradlew validateMcp` to verify all launchers.
- Launchers are located in `tooling/mcp/start-*.ps1`.
- Documentation: [docs/tooling/mcp-servers.md](tooling/mcp-servers.md).

## Testing Endpoints
We provide a suite of `.http` files in the `http/` directory.

Urutan manual test yang disarankan:
1. `http/01-smoke.http`
2. `http/10-auth.http`
3. `http/20-catalog.http`
4. `http/30-cart.http`
5. `http/40-checkout.http`
6. `http/50-order.http`
7. `http/90-webhooks.http`
8. `http/80-admin.http`

## Database Migrations
To check the status of your migrations:
```bash
./gradlew flywayInfoLocal
```
To run migrations manually:
```bash
./gradlew flywayMigrateLocal
```
To validate migrations:
```bash
./gradlew flywayValidateLocal
```
