import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import org.flywaydb.core.Flyway
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.7.4")
        classpath("org.flywaydb:flyway-core:10.20.1")
        classpath("org.flywaydb:flyway-database-postgresql:10.20.1")
    }
}

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.flywaydb.flyway") version "10.20.1"
}

group = "com.gayakini"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

// 1. Isolated configuration for Flyway Plugin Classpath
val flywayMigration by configurations.creating

dependencies {
    // App Dependencies
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")

    // Fix Flyway Plugin "No database found"
    flywayMigration("org.postgresql:postgresql:42.7.4")
    flywayMigration("org.flywaydb:flyway-core:10.20.1")
    flywayMigration("org.flywaydb:flyway-database-postgresql:10.20.1")

    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    // UPGRADED to 2.7.0 for Spring Boot 3.4 compatibility
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.h2database:h2")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

// --- QUALITY GATES ---

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    ignoreFailures = false
}

tasks.named("detekt") {
    mustRunAfter("detektBaseline")
}

// --- AUTOMATION: ENVIRONMENT & POSTGRES ---

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

val localEnv = loadDotEnv(projectDir)
localEnv.forEach { (k, v) -> System.setProperty(k, v) }

tasks.register("dbStart") {
    group = "database"
    description = "Checks and starts local PostgreSQL if needed (Windows only auto-start)."

    val dbPortValue = (System.getProperty("DB_PORT") ?: "5432").toInt()
    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    val userProfile = System.getenv("USERPROFILE")
    val pgDataVal =
        System.getProperty("PGDATA")
            ?: System.getenv("PGDATA")
            ?: "$userProfile\\scoop\\persist\\postgresql\\data"
    val pgDataFile = file(pgDataVal)

    // Captured services
    val execOps = serviceOf<ExecOperations>()

    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val ansiCyanLocal = "\u001B[36m"
        val ansiYellowLocal = "\u001B[33m"

        val isUp =
            try {
                Socket("localhost", dbPortValue).use { true }
            } catch (e: Exception) {
                false
            }

        if (isUp) {
            println("[$ansiGreenLocal\u2705$ansiResetLocal] PostgreSQL is already running on port $dbPortValue.")
        } else {
            if (!isWindows) {
                println("$ansiYellowLocal[SKIP]$ansiResetLocal PostgreSQL auto-start only on Windows.")
                return@doLast
            }
            println("[$ansiCyanLocal\uD83D\uDE80$ansiResetLocal] Attempting to start PostgreSQL via pg_ctl...")

            if (pgDataFile.exists()) {
                execOps.exec {
                    commandLine("cmd", "/c", "start", "/b", "pg_ctl", "-D", pgDataVal, "start")
                    isIgnoreExitValue = true
                }
                println("$ansiCyanLocal[WAIT]$ansiResetLocal Giving PostgreSQL 3 seconds to warm up...")
                Thread.sleep(3000)
            } else {
                println("$ansiYellowLocal[WARN]$ansiResetLocal PGDATA not found ($pgDataVal). Skipping.")
            }
        }
    }
}

// --- DEVELOPER WORKFLOW TASKS ---

tasks.register("devHelp") {
    group = "help"
    description = "Displays the local development workflow guide."
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val ansiCyanLocal = "\u001B[36m"
        val ansiBoldLocal = "\u001B[1m"
        println(
            """
            $ansiBoldLocal$ansiCyanLocal
              GAYAKINI BACKEND - DEVELOPER WORKFLOW$ansiResetLocal
              --------------------------------------
              1. $ansiGreenLocal./gradlew localSetup$ansiResetLocal      - Initial .env setup
              2. $ansiGreenLocal./gradlew dbDoctor$ansiResetLocal        - Database & env diagnostic check
              3. $ansiGreenLocal./gradlew bootRun$ansiResetLocal         - Run app with pre-flight checks
              4. $ansiGreenLocal./gradlew qualityCheck$ansiResetLocal    - Linting + Detekt + Unit Tests
              5. $ansiGreenLocal./gradlew validateMcp$ansiResetLocal     - Validate all MCP launchers
              6. $ansiGreenLocal./gradlew releaseCheck$ansiResetLocal    - Full quality gate (CI equivalent)
              7. $ansiGreenLocal./gradlew releaseCheckLocal$ansiResetLocal - Full gate + DB + MCP validation
            """.trimIndent(),
        )
    }
}

tasks.register("localSetup") {
    group = "setup"
    description = "Creates .env from .env.example if it doesn't exist."
    val exampleFile = file(".env.example")
    val targetFile = file(".env")
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val ansiCyanLocal = "\u001B[36m"
        if (exampleFile.exists() && !targetFile.exists()) {
            exampleFile.copyTo(targetFile)
            println("[$ansiGreenLocal\u2705$ansiResetLocal] .env file created.")
        } else if (targetFile.exists()) {
            println("[$ansiCyanLocal\u2139$ansiResetLocal] .env already exists.")
        }
    }
}

tasks.register("dbDoctor") {
    group = "verification"
    description = "Detailed database connectivity and environment check."
    dependsOn("dbStart")
    val envFileExists = file(".env").exists()
    val dbHostVal = System.getProperty("DB_HOST") ?: "localhost"
    val dbPortVal = (System.getProperty("DB_PORT") ?: "5432").toInt()
    val dbNameVal = System.getProperty("DB_NAME") ?: "gayakini"
    val javaVersion = System.getProperty("java.version")

    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val ansiRedLocal = "\u001B[31m"
        val ansiBoldLocal = "\u001B[1m"

        println("\n$ansiBoldLocal[DATABASE DIAGNOSTICS]$ansiResetLocal")

        print("Connectivity ($dbHostVal:$dbPortVal): ")
        try {
            Socket(dbHostVal, dbPortVal).use { println("$ansiGreenLocal UP$ansiResetLocal") }
        } catch (e: Exception) {
            println("$ansiRedLocal DOWN$ansiResetLocal (${e.message})")
        }

        println("Database Name: $dbNameVal")
        println("Java Version: $javaVersion")
        val envFound =
            if (envFileExists) {
                "$ansiGreenLocal FOUND$ansiResetLocal"
            } else {
                "$ansiRedLocal MISSING$ansiResetLocal"
            }
        println(".env file: $envFound")
    }
}

// --- CORE QUALITY GATE (No DB needed) ---

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs all code quality tools and unit tests."
    dependsOn("ktlintCheck", "detekt", "test")
}

// --- RELEASE GATE ---

tasks.register("releaseCheck") {
    group = "verification"
    description = "Full release quality gate (Clean + Quality + Build + MCP)."
    dependsOn("clean", "qualityCheck", "assemble", "validateMcp")
}

tasks.register("releaseCheckLocal") {
    group = "verification"
    description = "Full release quality gate including local DB migration validation."
    dependsOn("releaseCheck", "dbDoctor", "flywayValidateLocal")
}

// --- BOOTRUN & LOCAL RUN ---

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn("dbStart", "flywayMigrateLocal")
    localEnv.forEach { (k, v) -> environment(k, v) }
    systemProperty("spring.profiles.active", "local")
}

// --- TEST SUITES (Helpers) ---

tasks.register("smokeTest") {
    group = "verification"
    description = "Quick API health verification (requires running app)."
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val ansiRedLocal = "\u001B[31m"
        val ansiBoldLocal = "\u001B[1m"

        fun checkEndpointLocal(
            path: String,
            expectedStatus: Int = 200,
            name: String = "",
        ) {
            val url = URI("http://localhost:8080$path").toURL()
            print("Testing $path ${if (name.isNotEmpty()) "($name) " else ""}... ")
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                val status = connection.responseCode
                if (status == expectedStatus) {
                    println("$ansiGreenLocal PASSED ($status)$ansiResetLocal")
                } else {
                    println("$ansiRedLocal FAILED (Expected $expectedStatus, got $status)$ansiResetLocal")
                }
            } catch (e: Exception) {
                println("$ansiRedLocal ERROR (${e.message})$ansiResetLocal")
            }
        }

        println("\n$ansiBoldLocal[SMOKE TEST]$ansiResetLocal")
        checkEndpointLocal("/actuator/health", 200, "Health")
        checkEndpointLocal("/v1/hello", 200, "Hello")
        checkEndpointLocal("/swagger-ui.html", 200, "Swagger UI")
        checkEndpointLocal("/api-docs", 200, "OpenAPI Docs")
        checkEndpointLocal("/v1/products", 200, "Public Catalog")
    }
}

tasks.register("validateMcp") {
    group = "verification"
    description = "Validates all MCP launchers in -ValidateOnly mode."
    val projectDirStr = projectDir.absolutePath
    val execOps = serviceOf<ExecOperations>()

    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiYellowLocal = "\u001B[33m"
        val ansiBoldLocal = "\u001B[1m"

        if (!org.gradle.internal.os.OperatingSystem.current().isWindows) {
            println("$ansiYellowLocal[SKIP]$ansiResetLocal MCP validation only on Windows.")
            return@doLast
        }
        println("\n$ansiBoldLocal[MCP LAUNCHER VALIDATION]$ansiResetLocal")

        val mcpCommand =
            "\$env:PROJECT_ROOT='$projectDirStr'; " +
                "\$env:GITHUB_PERSONAL_ACCESS_TOKEN='dummy_token'; " +
                "Get-ChildItem 'tooling/mcp/start-*.ps1' | " +
                "Sort-Object Name | " +
                "ForEach-Object { " +
                "Write-Host \"`n--- Validating \$(\$_.Name) ---\" -ForegroundColor Cyan; " +
                "& powershell.exe -NoProfile -ExecutionPolicy Bypass -File \$_.FullName -ValidateOnly }"

        execOps.exec {
            commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", mcpCommand)
        }
    }
}

// --- FLYWAY ---

fun createLocalFlyway(pDir: File): Flyway {
    val host = System.getProperty("DB_HOST") ?: "localhost"
    val port = System.getProperty("DB_PORT") ?: "5432"
    val name = System.getProperty("DB_NAME") ?: "gayakini"
    val defaultUrl = "jdbc:postgresql://$host:$port/$name"
    val dbUrl = System.getProperty("DB_URL") ?: defaultUrl
    val user = System.getProperty("DB_USERNAME") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: "password"

    return Flyway.configure()
        .dataSource(dbUrl, user, password)
        .schemas("commerce", "public")
        .defaultSchema("commerce")
        .baselineOnMigrate(true)
        .locations("filesystem:$pDir/src/main/resources/db/migration")
        .load()
}

tasks.register("flywayInfoLocal") {
    group = "database"
    description = "Shows local Flyway migration status."
    dependsOn("dbStart")
    val pDir = projectDir
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiBoldLocal = "\u001B[1m"
        val flyway = createLocalFlyway(pDir)
        val info = flyway.info()
        println("\n$ansiBoldLocal[FLYWAY INFO LOCAL]$ansiResetLocal")
        info.all().forEach { migration ->
            println("${migration.state} | ${migration.version ?: "<<repeatable>>"} | ${migration.description}")
        }
    }
}

tasks.register("flywayMigrateLocal") {
    group = "database"
    description = "Runs Flyway migrations against local database."
    dependsOn("dbStart")
    val pDir = projectDir
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val result = createLocalFlyway(pDir).migrate()
        val count = result.migrationsExecuted
        println("[$ansiGreenLocal\u2705$ansiResetLocal] Flyway migrate local complete. Migrations executed: $count")
    }
}

tasks.register("flywayValidateLocal") {
    group = "verification"
    description = "Validates local migrations against local database."
    dependsOn("dbStart")
    val pDir = projectDir
    doLast {
        val ansiResetLocal = "\u001B[0m"
        val ansiGreenLocal = "\u001B[32m"
        val result = createLocalFlyway(pDir).validateWithResult()
        if (!result.validationSuccessful) {
            val invalid = result.invalidMigrations.firstOrNull()
            val message = invalid?.errorDetails?.errorMessage ?: "Flyway validation gagal."
            throw GradleException(message)
        }
        println("[$ansiGreenLocal\u2705$ansiResetLocal] Flyway validation local passed.")
    }
}

flyway {
    driver = "org.postgresql.Driver"
    val host = System.getProperty("DB_HOST") ?: "localhost"
    val port = System.getProperty("DB_PORT") ?: "5432"
    val name = System.getProperty("DB_NAME") ?: "gayakini"
    val defaultDbUrl = "jdbc:postgresql://$host:$port/$name"
    url = System.getProperty("DB_URL") ?: defaultDbUrl
    user = System.getProperty("DB_USERNAME") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: "password"
    schemas = arrayOf("commerce", "public")
    defaultSchema = "commerce"
    createSchemas = true
    baselineOnMigrate = true
    configurations = arrayOf("runtimeClasspath", "flywayMigration")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
