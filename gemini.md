# Gemini Agent Context: gayakini

You are an expert Kotlin and Spring Boot developer.
Repository Root: `C:\Software\gayakini`

## Operational Context
- **Backend:** Spring Boot 3.4, Kotlin 2.0.
- **Database:** PostgreSQL with Flyway migrations.
- **Tools:** Gradle, ktlint, detekt.
- **Key Integrations:** Midtrans (Payments), Biteship (Logistics).

## MCP Server Rules
1. **gayakini-filesystem:**
   - Always restrict reads/writes to `C:\Software\gayakini`.
   - **AIDA CRITICAL:** Use ONLY `{"path": "..."}` for `read_file`. No extra keys.
2. **gayakini-github:**
   - **AIDA CRITICAL:** Use ONLY minimal keys for `create_or_update_file`: `owner`, `repo`, `path`, `content`, `message`.
3. **gayakini-terminal:**
   - Use for `./gradlew` commands.
   - Prefer `./gradlew check` for quick validation.
4. **gayakini-postgres:**
   - Use to verify schema in `public` or `history` schemas.
   - Do not perform destructive `DROP` or `TRUNCATE` without confirmation.

## Coding Standards
- Use `UUIDv7` for primary keys (via `com.github.f4b6a3:uuid-creator`).
- Domain logic belongs in `application` or `domain` packages.
- Entities must use JPA annotations correctly with Kotlin.
- Ensure all new API endpoints are documented in OpenAPI/Swagger.

## Verification Workflow
Before task completion:
1. Run `./gradlew ktlintCheck detekt`
2. Run `./gradlew test`
3. Verify database migrations if schema changed.
