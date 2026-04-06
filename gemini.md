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
   - Validate launcher first: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly`
2. **gayakini-github:**
   - **AIDA CRITICAL:** Use ONLY minimal keys for `create_or_update_file`: `owner`, `repo`, `path`, `content`, `message`.
   - Token precedence: `GITHUB_PERSONAL_ACCESS_TOKEN`, `GITHUB_TOKEN`, `GH_TOKEN`.
3. **gayakini-terminal:**
   - Use for `./gradlew` commands.
   - Prefer `./gradlew clean ktlintCheck detekt test build` for evidence.
4. **gayakini-postgres:**
   - Use to verify schema in `public` or `history` schemas.
   - Do not perform destructive `DROP` or `TRUNCATE` without confirmation.
   - Local defaults: `localhost:5432`, database `gayakini`, user `postgres`, password `password`.
5. **Launcher behavior:**
   - All launcher scripts live under `tooling\mcp`.
   - For Codex local MCP, use Windows PowerShell directly, not WSL.
   - Use `-ValidateOnly` for smoke checks because real MCP launchers must keep STDIO clean.

## Coding Standards
- Use `UUIDv7` for primary keys (via `com.github.f4b6a3:uuid-creator`).
- Domain logic belongs in `application` or `domain` packages.
- Entities must use JPA annotations correctly with Kotlin.
- Ensure all new API endpoints are documented in OpenAPI/Swagger.

## Verification Workflow
Before task completion:
1. Run `./gradlew clean`
2. Run `./gradlew ktlintCheck`
3. Run `./gradlew detekt`
4. Run `./gradlew test`
5. Run `./gradlew build`
6. Validate each launcher in `tooling\mcp\start-*.ps1` with `-ValidateOnly`
