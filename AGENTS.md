# Agent Guidelines: gayakini Backend

## Repository Root: `C:\Software\gayakini`

## CRITICAL: Security & Secret Scanning
- **DO NOT COMMIT SECRETS:** File `.env`, `local.env`, dan file environment lainnya terlarang untuk masuk ke Git.
- **Git Hygiene:** Jika terjadi kebocoran secret ke remote repository, segera rotate key tersebut di provider terkait.

## CRITICAL: AIDA / MCP Tool Rules
Saat menggunakan MCP tools, gunakan input seminimal mungkin.
- **`read_file`:** Cukup `{"path": "..."}`.
- **`create_or_update_file`:** Gunakan hanya `owner`, `repo`, `path`, `content`, `message`.

## Source of Truth Hierarchy
1. **Code Implementation** (Kotlin/Java)
2. **Flyway Migrations** (Database schema)
3. **OpenAPI / Contracts** (API Agreement)
4. **Markdown Documentation** (Architectural intent)

## Rules of Engagement
- **Architecture:** Modular Monolith. Tetap sederhana, hindari over-engineering.
- **Evidence-Based:** Jangan berasumsi build/test sukses tanpa menjalankan perintah Gradle.
- **Database:** Gunakan `UUIDv7`. Jangan edit file migrasi yang sudah rilis (Commit baru untuk perubahan).
- **Idempotency:** Wajib untuk Place Order, Payment, dan Webhook processing.

## Development Verification Flow
Gunakan `gayakini-terminal` untuk verifikasi:
1. `./gradlew clean`
2. `./gradlew ktlintCheck`
3. `./gradlew detekt`
4. `./gradlew test`
5. `./gradlew build`
