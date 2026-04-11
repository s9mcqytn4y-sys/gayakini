import java.net.Socket
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Robust .env loader that does NOT mutate global System properties.
 */
fun loadDotEnv(projectDir: File): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val dotEnv = File(projectDir, ".env")
    if (dotEnv.exists()) {
        dotEnv.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) envMap[parts[0].trim()] = parts[1].trim()
            }
        }
    }
    return envMap
}

// Pre-load environment for configuration phase and share via extra properties
val env = loadDotEnv(project.rootDir)
project.extra.set("localEnv", env)

tasks.register("dbStart") {
    group = "database"
    description = "Starts local PostgreSQL if needed (Windows only auto-start)."

    // Capture state to avoid Project serialization
    val dbPortValue = (env["DB_PORT"] ?: "5432").toInt()
    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    val userProfile = System.getenv("USERPROFILE")
    val pgDataVal = env["PGDATA"] ?: "$userProfile\\scoop\\persist\\postgresql\\data"
    val pgDataFile = file(pgDataVal)

    doLast {
        val isUp = try {
            Socket("localhost", dbPortValue).use { true }
        } catch (e: Exception) {
            false
        }

        if (isUp) {
            println("[\u2705] PostgreSQL is already running on port $dbPortValue.")
        } else if (isWindows && pgDataFile.exists()) {
            println("[\uD83D\uDE80] Attempting to start PostgreSQL...")
            // Avoid calling project.serviceOf in doLast for config cache
            val execOps = project.serviceOf<ExecOperations>()
            execOps.exec {
                commandLine("cmd", "/c", "start", "/b", "pg_ctl", "-D", pgDataVal, "start")
                isIgnoreExitValue = true
            }
            Thread.sleep(3000)
        }
    }
}

tasks.register("localSetup") {
    group = "setup"
    description = "Creates .env from .env.example if it doesn't exist."
    val exampleFile = file("${project.rootDir}/.env.example")
    val targetFile = file("${project.rootDir}/.env")
    doLast {
        if (exampleFile.exists() && !targetFile.exists()) {
            exampleFile.copyTo(targetFile)
            println("[\u2705] .env file created.")
        } else if (targetFile.exists()) {
            println("[\u2139] .env already exists.")
        }
    }
}
