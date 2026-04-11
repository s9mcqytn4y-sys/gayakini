# Gemini Agent Context: gayakini

`AGENTS.md` adalah source of truth operasional. File ini hanya overlay Gemini dan tidak boleh bertentangan dengannya.

You are an expert Kotlin and Spring Boot developer.
Repository Root: `C:\Software\gayakini`

## Operational Context
- **Backend:** Spring Boot 3.4, Kotlin 2.0.
- **Database:** PostgreSQL 18+ with Flyway migrations.
- **Tools:** Gradle 8.11+, ktlint, detekt.
- **Key Integrations:** Midtrans (Payments), Biteship (Logistics).
- **Quality Gate:** `./gradlew check` (Ktlint + Detekt).

## Documentation Hierarchy
1. **Source Code:** Implementation truth.
2. **OpenAPI (`docs/brand-fashion-ecommerce-api-final.yaml`):** API Contract truth.
3. **`AGENTS.md`:** Operational agent/MCP truth.
4. **`CONTRIBUTING.md`:** Developer guidelines & release gates.
5. **`CHANGELOG.md`:** Versioned changes and history.

## MCP Server Rules
1. **gayakini-filesystem:**
   - Always restrict reads/writes to `C:\Software\gayakini`.
   - **AIDA CRITICAL:** Use ONLY `{"path": "..."}` for `read_file`. No extra keys.
2. **gayakini-http:**
   - Default ke `APP_BASE_URL=http://localhost:8080`.
   - Default spec adalah `docs\brand-fashion-ecommerce-api-final.yaml`.
   - Treat as local OpenAPI helper, not production API bridge.
3. **gayakini-browser:**
   - Package target adalah Puppeteer MCP server.
   - Launch options are package-managed.
4. **gayakini-github:**
   - **AIDA CRITICAL:** Use ONLY minimal keys for `create_or_update_file`: `owner`, `repo`, `path`, `content`, `message`.
   - Token precedence: `GITHUB_PERSONAL_ACCESS_TOKEN`, `GITHUB_TOKEN`, `GH_TOKEN`.
5. **gayakini-terminal:**
   - Use for `./gradlew` commands.
   - Use `./gradlew check` for core quality evidence.
   - **RTK Filter (V2):** Use `.\tooling\rtk\rtk.ps1 -Cmd "..."` to reduce token noise from long CLI outputs.
   - **Benchmark:** Add `-Benchmark` to see noise reduction stats.
   - **Logs:** Refer to `tooling/rtk/logs/` if you need the full raw output.
6. **gayakini-postgres:**
   - Use to verify schema in `commerce` or `public` schemas.
   - Local defaults: `localhost:5432`, database `gayakini`, user `postgres`, password `password`.
7. **Launcher behavior:**
   - All launcher scripts live under `tooling\mcp`.

## Coding Standards
- Use `UUIDv7` for primary keys.
- Domain logic in `application` or `domain`.
- **Logging:** Use structured logging with `com.gayakini` at DEBUG level (configured in `application.yml`).
- **Health:** Actuator endpoints enabled (`/actuator/health`, `/actuator/info`).

## Verification Workflow
Before task completion:
1. Run `./gradlew clean`
2. Run `./gradlew build`
3. Verify documentation sync (README, CHANGELOG, AGENTS.md).

## Troubleshooting
1. Start from `./gradlew check`.
2. For Gradle stuck, check if PostgreSQL is running locally.
3. For GitHub issues, check token source only, never commit token values.
