import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.flywaydb.core.Flyway
import java.net.Socket
import java.net.HttpURLConnection
import java.net.URL

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

val ANSI_RESET = "\u001B[0m"
val ANSI_GREEN = "\u001B[32m"
val ANSI_YELLOW = "\u001B[33m"
val ANSI_CYAN = "\u001B[36m"
val ANSI_BOLD = "\u001B[1m"
val ANSI_RED = "\u001B[31m"

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
    ignoreFailures.set(true)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    ignoreFailures = true
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

tasks.register("ensurePostgres") {
    group = "database"
    description = "Checks and starts local PostgreSQL."
    doLast {
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        val isUp = try { Socket("localhost", dbPort).use { true } } catch (e: Exception) { false }

        if (isUp) {
            println("[$ANSI_GREEN\u2705$ANSI_RESET] PostgreSQL is already running on port $dbPort.")
        } else {
            if (!org.gradle.internal.os.OperatingSystem.current().isWindows) {
                throw GradleException("PostgreSQL is not running. Please start it manually.")
            }
            println("[$ANSI_CYAN\uD83D\uDE80$ANSI_RESET] Attempting to start PostgreSQL via pg_ctl...")
            val userProfile = System.getenv("USERPROFILE")
            val pgData = System.getenv("PGDATA") ?: "$userProfile\\scoop\\persist\\postgresql\\data"

            if (file(pgData).exists()) {
                project.exec {
                    commandLine("cmd", "/c", "pg_ctl", "-D", pgData, "start")
                    isIgnoreExitValue = true
                }
                Thread.sleep(3000)
            } else {
                println("$ANSI_YELLOW[WARN]$ANSI_RESET PGDATA not found. Skipping auto-start. Ensure DB is running.")
            }
        }
    }
}

// --- DEVELOPER WORKFLOW TASKS ---

tasks.register("devHelp") {
    group = "help"
    description = "Displays the local development workflow guide."
    doLast {
        println("""
$ANSI_BOLD$ANSI_CYAN
  GAYAKINI BACKEND - DEVELOPER WORKFLOW$ANSI_RESET
  --------------------------------------
  1. $ANSI_GREEN./gradlew localSetup$ANSI_RESET      - Initial .env setup
  2. $ANSI_GREEN./gradlew doctor$ANSI_RESET          - Diagnostic check
  3. $ANSI_GREEN./gradlew bootRun$ANSI_RESET         - Run app with pre-flight checks
  4. $ANSI_GREEN./gradlew smokeTest$ANSI_RESET       - Quick API health verification
  5. $ANSI_GREEN./gradlew apiTest$ANSI_RESET         - Public API verification
  6. $ANSI_GREEN./gradlew rbacTest$ANSI_RESET        - Security/RBAC verification
  7. $ANSI_GREEN./gradlew releaseCheck$ANSI_RESET    - Full quality gate (advisory quality tools + tests + Flyway)

  $ANSI_YELLOW  Swagger UI: http://localhost:8080/swagger-ui.html
  API Docs:   http://localhost:8080/api-docs
  API Base:   http://localhost:8080/v1$ANSI_RESET
        """.trimIndent())
    }
}

tasks.register("localSetup") {
    group = "setup"
    doLast {
        val example = file(".env.example")
        val target = file(".env")
        if (example.exists() && !target.exists()) {
            example.copyTo(target)
            println("[$ANSI_GREEN\u2705$ANSI_RESET] .env file created.")
        }
    }
}

tasks.register("doctor") {
    group = "verification"
    dependsOn("ensurePostgres")
    doLast {
        println("\n$ANSI_BOLD[DIAGNOSTICS]$ANSI_RESET")
        val dbHost = System.getProperty("DB_HOST") ?: "localhost"
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        print("Database ($dbHost:$dbPort): ")
        try { Socket(dbHost, dbPort).use { println("${ANSI_GREEN}UP${ANSI_RESET}") } } catch (e: Exception) { println("${ANSI_RED}DOWN${ANSI_RESET}") }

        println("Java Version: ${System.getProperty("java.version")}")
        println(".env file: ${if (file(".env").exists()) "${ANSI_GREEN}FOUND${ANSI_RESET}" else "${ANSI_RED}MISSING${ANSI_RESET}"}")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn("ensurePostgres", "flywayMigrateLocal")
    localEnv.forEach { (k, v) -> environment(k, v) }
    systemProperty("spring.profiles.active", "local")
}

// --- TEST SUITES ---

fun checkEndpoint(path: String, expectedStatus: Int = 200, name: String = "") {
    val url = URL("http://localhost:8080$path")
    print("Testing $path ${if (name.isNotEmpty()) "($name) " else ""}... ")
    try {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        val status = connection.responseCode
        if (status == expectedStatus) {
            println("${ANSI_GREEN}PASSED ($status)${ANSI_RESET}")
        } else {
            println("${ANSI_RED}FAILED (Expected $expectedStatus, got $status)${ANSI_RESET}")
        }
    } catch (e: Exception) {
        println("${ANSI_RED}ERROR (${e.message})${ANSI_RESET}")
    }
}

tasks.register("smokeTest") {
    group = "verification"
    doLast {
        println("\n$ANSI_BOLD[SMOKE TEST]$ANSI_RESET")
        checkEndpoint("/actuator/health", 200, "Health")
        checkEndpoint("/v1/hello", 200, "Hello")
        checkEndpoint("/swagger-ui.html", 200, "Swagger UI")
        checkEndpoint("/api-docs", 200, "OpenAPI Docs")
        checkEndpoint("/v1/products", 200, "Public Catalog")
    }
}

tasks.register("apiTest") {
    group = "verification"
    description = "Verifies basic business flows (Public)."
    doLast {
        println("\n$ANSI_BOLD[API BUSINESS TEST]$ANSI_RESET")
        checkEndpoint("/v1/locations/areas", 200, "Locations")
        // Add more sequence tests here or point to .http
        println("Recommended: Use IntelliJ HTTP Client for full business flows in /http/*.http")
    }
}

tasks.register("rbacTest") {
    group = "verification"
    description = "Verifies RBAC security constraints."
    doLast {
        println("\n$ANSI_BOLD[RBAC SECURITY TEST]$ANSI_RESET")
        checkEndpoint("/v1/admin/products", 401, "Admin Deny Anonymous")
        checkEndpoint("/v1/me", 401, "Customer Deny Anonymous")
    }
}

tasks.register("verifyMigrations") {
    group = "verification"
    dependsOn("ensurePostgres", "flywayValidateLocal")
}

tasks.register("releaseCheck") {
    group = "verification"
    dependsOn("doctor", "ktlintCheck", "detekt", "test", "flywayValidateLocal")
}

// --- FLYWAY ---

fun createLocalFlyway(): Flyway {
    val url =
        System.getProperty("DB_URL")
            ?: "jdbc:postgresql://${System.getProperty("DB_HOST") ?: "localhost"}:${System.getProperty("DB_PORT") ?: "5432"}/${System.getProperty("DB_NAME") ?: "gayakini"}"
    val user = System.getProperty("DB_USERNAME") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: "password"

    return Flyway.configure()
        .dataSource(url, user, password)
        .schemas("commerce", "public")
        .defaultSchema("commerce")
        .baselineOnMigrate(true)
        .locations("filesystem:${project.projectDir}/src/main/resources/db/migration")
        .load()
}

tasks.register("flywayInfoLocal") {
    group = "database"
    dependsOn("ensurePostgres")
    doLast {
        val flyway = createLocalFlyway()
        val info = flyway.info()
        println("\n$ANSI_BOLD[FLYWAY INFO LOCAL]$ANSI_RESET")
        info.all().forEach { migration ->
            println("${migration.state} | ${migration.version ?: "<<repeatable>>"} | ${migration.description}")
        }
    }
}

tasks.register("flywayMigrateLocal") {
    group = "database"
    dependsOn("ensurePostgres")
    doLast {
        val result = createLocalFlyway().migrate()
        println("[$ANSI_GREEN\u2705$ANSI_RESET] Flyway migrate local complete. Migrations executed: ${result.migrationsExecuted}")
    }
}

tasks.register("flywayValidateLocal") {
    group = "verification"
    dependsOn("ensurePostgres")
    doLast {
        val result = createLocalFlyway().validateWithResult()
        if (!result.validationSuccessful) {
            val message = result.invalidMigrations.firstOrNull()?.errorDetails?.errorMessage ?: "Flyway validation gagal."
            throw GradleException(message)
        }
        println("[$ANSI_GREEN\u2705$ANSI_RESET] Flyway validation local passed.")
    }
}

flyway {
    driver = "org.postgresql.Driver"
    url = System.getProperty("DB_URL") ?: "jdbc:postgresql://${System.getProperty("DB_HOST") ?: "localhost"}:${System.getProperty("DB_PORT") ?: "5432"}/${System.getProperty("DB_NAME") ?: "gayakini"}"
    user = System.getProperty("DB_USERNAME") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: "password"
    schemas = arrayOf("commerce", "public")
    defaultSchema = "commerce"
    createSchemas = true
    baselineOnMigrate = true
    configurations = arrayOf("runtimeClasspath", "flywayMigration")
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
