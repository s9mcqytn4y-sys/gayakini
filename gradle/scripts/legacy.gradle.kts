import java.net.Socket
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf
import java.sql.DriverManager

// Helper to get environment from the main project extra properties
@Suppress("UNCHECKED_CAST")
fun getLocalEnv(): Map<String, String> {
    return project.extra.get("localEnv") as Map<String, String>
}

tasks.register("dbVerify") {
    group = "legacy"
    val env = getLocalEnv()
    doLast {
        val host = env["DB_HOST"] ?: "localhost"
        val port = (env["DB_PORT"] ?: "5432").toInt()
        try {
            Socket(host, port).use { println("[✅] Database connectivity OK: $host:$port") }
        } catch (e: Exception) {
            println("[❌] Database connectivity FAILED: $host:$port (${e.message})")
        }
    }
}

tasks.register("dbSeed") {
    group = "legacy"
    dependsOn("dbVerify")
    val env = getLocalEnv()
    doLast {
        val host = env["DB_HOST"] ?: "localhost"
        val port = env["DB_PORT"] ?: "5432"
        val name = env["DB_NAME"] ?: "gayakini"
        val url = env["DB_URL"] ?: "jdbc:postgresql://$host:$port/$name"
        val user = env["DB_USERNAME"] ?: "postgres"
        val pass = env["DB_PASSWORD"] ?: "password"

        DriverManager.getConnection(url, user, pass).use { conn ->
            val stmt = conn.createStatement()
            println("[📦] Seeding default admin...")
            stmt.execute(
                """
                INSERT INTO commerce.customers (id, email, password_hash, full_name, phone, created_at, updated_at)
                VALUES ('01918384-2591-789a-9e7b-7b3b7b3b7b3b', 'admin@gayakini.com',
                        '${'$'}2a${'$'}12${'$'}S7q1N5R6Uj9WvLz.Yf8mHeU8X.k4a2hR.QzC1vY1V/RzYwN2yRzY.',
                        'Gayakini Admin', '+628123456789', now(), now())
                ON CONFLICT (email) DO NOTHING
                """.trimIndent(),
            )
            println("[✅] Seeding complete.")
        }
    }
}

tasks.register("dbDoctor") {
    group = "legacy"
    dependsOn("dbStart", "flywayMigrateLocal", "dbVerify", "dbSeed")
}

// flywayMigrateLocal removed from here as it requires Flyway library in the buildscript classpath,
// which is tricky for a script plugin without proper setup.
// Use standard 'flywayMigrate' or define it in build.gradle.kts if needed.

tasks.register("validateMcp") {
    group = "legacy"
    val pDir = project.rootDir.absolutePath
    val execOps = project.serviceOf<ExecOperations>()
    doLast {
        val cmd = "\u0024env:PROJECT_ROOT='$pDir'; \u0024env:GITHUB_PERSONAL_ACCESS_TOKEN='dummy'; Get-ChildItem 'tooling/mcp/start-*.ps1' | ForEach-Object { & powershell.exe -NoProfile -ExecutionPolicy Bypass -File \u0024_.FullName -ValidateOnly }"
        execOps.exec {
            commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", cmd)
        }
    }
}

tasks.register("releaseCheck") {
    group = "legacy"
    description = "Legacy release check (Clean + Quality + Assemble + MCP)"
    dependsOn("clean", "check", "assemble", "validateMcp")
}
