import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import org.flywaydb.core.Flyway
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

val ansiReset = "\u001B[0m"
val ansiGreen = "\u001B[32m"
val ansiYellow = "\u001B[33m"
val ansiCyan = "\u001B[36m"
val ansiBold = "\u001B[1m"
val ansiRed = "\u001B[31m"

// 1. CRITICAL: Isolated configuration for Flyway Plugin Classpath
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

    // 2. CRITICAL: Fix Flyway Plugin "No database found"
    flywayMigration("org.postgresql:postgresql:42.7.4")
    flywayMigration("org.flywaydb:flyway-core:10.20.1")
    flywayMigration("org.flywaydb:flyway-database-postgresql:10.20.1")

    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
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

fun loadDotEnv(): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val dotEnv = file(".env")
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

val localEnv = loadDotEnv()
localEnv.forEach { (k, v) -> System.setProperty(k, v) }

tasks.register("dbStart") {
    group = "database"
    description = "Checks and starts local PostgreSQL if needed (Windows only auto-start)."
    doLast {
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        val isUp =
            try {
                Socket("localhost", dbPort).use { true }
            } catch (e: Exception) {
                false
            }

        if (isUp) {
            println("[$ansiGreen\u2705$ansiReset] PostgreSQL is already running on port $dbPort.")
        } else {
            if (!org.gradle.internal.os.OperatingSystem.current().isWindows) {
                println("$ansiYellow[SKIP]$ansiReset PostgreSQL is not running. Auto-start only supported on Windows.")
                return@doLast
            }
            println("[$ansiCyan\uD83D\uDE80$ansiReset] Attempting to start PostgreSQL via pg_ctl...")
            val userProfile = System.getenv("USERPROFILE")
            val pgData = System.getenv("PGDATA") ?: "$userProfile\\scoop\\persist\\postgresql\\data"

            if (file(pgData).exists()) {
                project.exec {
                    commandLine("cmd", "/c", "pg_ctl", "-D", pgData, "start")
                    isIgnoreExitValue = true
                }
                println("$ansiCyan[WAIT]$ansiReset Giving PostgreSQL 3 seconds to warm up...")
                Thread.sleep(3000)
            } else {
                println(
                    "$ansiYellow[WARN]$ansiReset PGDATA not found ($pgData). Skipping auto-start. Ensure DB is running.",
                )
            }
        }
    }
}

// --- DEVELOPER WORKFLOW TASKS ---

tasks.register("devHelp") {
    group = "help"
    description = "Displays the local development workflow guide."
    doLast {
        println(
            """
            $ansiBold$ansiCyan
              GAYAKINI BACKEND - DEVELOPER WORKFLOW$ansiReset
              --------------------------------------
              1. $ansiGreen./gradlew localSetup$ansiReset      - Initial .env setup
              2. $ansiGreen./gradlew dbDoctor$ansiReset        - Database & env diagnostic check
              3. $ansiGreen./gradlew bootRun$ansiReset         - Run app with pre-flight checks
              4. $ansiGreen./gradlew qualityCheck$ansiReset    - Linting + Detekt + Unit Tests
              5. $ansiGreen./gradlew validateMcp$ansiReset     - Validate all MCP launchers
              6. $ansiGreen./gradlew releaseCheck$ansiReset    - Full quality gate (CI equivalent)
              7. $ansiGreen./gradlew releaseCheckLocal$ansiReset - Full gate + DB + MCP validation

              $ansiYellow  Swagger UI: http://localhost:8080/swagger-ui.html
              API Docs:   http://localhost:8080/api-docs
              API Base:   http://localhost:8080/v1$ansiReset
            """.trimIndent(),
        )
    }
}

tasks.register("localSetup") {
    group = "setup"
    description = "Creates .env from .env.example if it doesn't exist."
    doLast {
        val example = file(".env.example")
        val target = file(".env")
        if (example.exists() && !target.exists()) {
            example.copyTo(target)
            println("[$ansiGreen\u2705$ansiReset] .env file created.")
        } else if (target.exists()) {
            println("[$ansiCyan\u2139$ansiReset] .env already exists.")
        }
    }
}

tasks.register("dbDoctor") {
    group = "verification"
    description = "Detailed database connectivity and environment check."
    dependsOn("dbStart")
    doLast {
        println("\n$ansiBold[DATABASE DIAGNOSTICS]$ansiReset")
        val dbHost = System.getProperty("DB_HOST") ?: "localhost"
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        val dbName = System.getProperty("DB_NAME") ?: "gayakini"

        print("Connectivity ($dbHost:$dbPort): ")
        try {
            Socket(dbHost, dbPort).use { println("$ansiGreen UP$ansiReset") }
        } catch (e: Exception) {
            println("$ansiRed DOWN$ansiReset (${e.message})")
        }

        println("Database Name: $dbName")
        println("Java Version: ${System.getProperty("java.version")}")
        println(
            ".env file: ${if (file(".env").exists()) {
                "$ansiGreen FOUND$ansiReset"
            } else {
                "$ansiRed MISSING$ansiReset"
            }}",
        )
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
    // We make DB setup explicit but non-blocking for bootRun if the user wants to handle it
    // But for "magical" experience, we keep it but ensure it's healthy
    dependsOn("dbStart", "flywayMigrateLocal")
    localEnv.forEach { (k, v) -> environment(k, v) }
    systemProperty("spring.profiles.active", "local")
}

// --- TEST SUITES (Helpers) ---

fun checkEndpoint(
    path: String,
    expectedStatus: Int = 200,
    name: String = "",
) {
    val url = URL("http://localhost:8080$path")
    print("Testing $path ${if (name.isNotEmpty()) "($name) " else ""}... ")
    try {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        val status = connection.responseCode
        if (status == expectedStatus) {
            println("$ansiGreen PASSED ($status)$ansiReset")
        } else {
            println("$ansiRed FAILED (Expected $expectedStatus, got $status)$ansiReset")
        }
    } catch (e: Exception) {
        println("$ansiRed ERROR (${e.message})$ansiReset")
    }
}

tasks.register("smokeTest") {
    group = "verification"
    description = "Quick API health verification (requires running app)."
    doLast {
        println("\n$ansiBold[SMOKE TEST]$ansiReset")
        checkEndpoint("/actuator/health", 200, "Health")
        checkEndpoint("/v1/hello", 200, "Hello")
        checkEndpoint("/swagger-ui.html", 200, "Swagger UI")
        checkEndpoint("/api-docs", 200, "OpenAPI Docs")
        checkEndpoint("/v1/products", 200, "Public Catalog")
    }
}

tasks.register("validateMcp") {
    group = "verification"
    description = "Validates all MCP launchers in -ValidateOnly mode."
    doLast {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows) {
            println("$ansiYellow[SKIP]$ansiReset MCP launcher validation currently only supported on Windows.")
            return@doLast
        }
        println("\n$ansiBold[MCP LAUNCHER VALIDATION]$ansiReset")

        val mcpCommand =
            "\$env:PROJECT_ROOT='$projectDir'; " +
                "\$env:GITHUB_PERSONAL_ACCESS_TOKEN='dummy_token'; " +
                "Get-ChildItem 'tooling/mcp/start-*.ps1' | " +
                "Sort-Object Name | " +
                "ForEach-Object { Write-Host \"`n--- Validating \$(\$_.Name) ---\" -ForegroundColor Cyan; " +
                "& powershell.exe -NoProfile -ExecutionPolicy Bypass -File \$_.FullName -ValidateOnly }"

        project.exec {
            commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", mcpCommand)
        }
    }
}

// --- FLYWAY ---

fun createLocalFlyway(): Flyway {
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
        .locations("filesystem:${project.projectDir}/src/main/resources/db/migration")
        .load()
}

tasks.register("flywayInfoLocal") {
    group = "database"
    description = "Shows local Flyway migration status."
    dependsOn("dbStart")
    doLast {
        val flyway = createLocalFlyway()
        val info = flyway.info()
        println("\n$ansiBold[FLYWAY INFO LOCAL]$ansiReset")
        info.all().forEach { migration ->
            println("${migration.state} | ${migration.version ?: "<<repeatable>>"} | ${migration.description}")
        }
    }
}

tasks.register("flywayMigrateLocal") {
    group = "database"
    description = "Runs Flyway migrations against local database."
    dependsOn("dbStart")
    doLast {
        val result = createLocalFlyway().migrate()
        val count = result.migrationsExecuted
        println("[$ansiGreen\u2705$ansiReset] Flyway migrate local complete. Migrations executed: $count")
    }
}

tasks.register("flywayValidateLocal") {
    group = "verification"
    description = "Validates local migrations against local database."
    dependsOn("dbStart")
    doLast {
        val result = createLocalFlyway().validateWithResult()
        if (!result.validationSuccessful) {
            val invalid = result.invalidMigrations.firstOrNull()
            val message = invalid?.errorDetails?.errorMessage ?: "Flyway validation gagal."
            throw GradleException(message)
        }
        println("[$ansiGreen\u2705$ansiReset] Flyway validation local passed.")
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
