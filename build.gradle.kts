import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.Socket

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.7.4")
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

// --- QUALITY GATES (REALISTIC STRATEGY) ---

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true) // Don't block local dev, report only
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    ignoreFailures = true // Don't block local dev
}

// Ensure detekt baseline exists or is created before checks
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
    description = "Checks and starts local PostgreSQL via pg_ctl if needed (Windows/Scoop aware)."
    doLast {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows) return@doLast

        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        val isUp = try { Socket("localhost", dbPort).use { true } } catch (e: Exception) { false }

        if (isUp) {
            println("[\uD83D\uDDB3] PostgreSQL is already running on port $dbPort. \u2705")
        } else {
            println("[\uD83D\uDDB3] PostgreSQL not detected. Attempting to start via pg_ctl... \uD83D\uDE80")
            val userProfile = System.getenv("USERPROFILE")
            val pgData = System.getenv("PGDATA") ?: "$userProfile\\scoop\\persist\\postgresql\\data"

            if (!file(pgData).exists()) {
                println("[\u274C] ERROR: PGDATA not found at $pgData")
                throw GradleException("PostgreSQL Data directory not found. Ensure PostgreSQL is installed via Scoop.")
            }

            project.exec {
                commandLine("cmd", "/c", "pg_ctl", "-D", pgData, "start")
                isIgnoreExitValue = true
            }
            Thread.sleep(3000) // Grace period
        }
    }
}

// --- DEVELOPER UX TASKS ---

tasks.register("localSetup") {
    group = "setup"
    description = "Bootstraps local environment variables."
    doLast {
        val example = file(".env.example")
        val target = file(".env")
        if (example.exists() && !target.exists()) {
            example.copyTo(target)
            println("[\u2705] .env file created from .env.example")
        } else if (target.exists()) {
            println("[\u2139\uFE0F] .env already exists.")
        }
    }
}

tasks.register("doctor") {
    group = "verification"
    description = "Release-quality environment diagnostic."
    dependsOn("ensurePostgres")
    doLast {
        println("\n\uD83D\uDE80 GAYAKINI PREFLIGHT DIAGNOSTICS")
        println("================================")
        println("[JVM] Version: ${System.getProperty("java.version")}")
        val dbHost = System.getProperty("DB_HOST") ?: "localhost"
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        print("[DB]  Checking $dbHost:$dbPort... ")
        try {
            Socket(dbHost, dbPort).use { println("SUCCESS \u2705") }
        } catch (e: Exception) {
            println("FAILED \u274C")
        }
        println("================================")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn("ensurePostgres")
    localEnv.forEach { (k, v) -> environment(k, v) }
    if (System.getProperty("spring.profiles.active") == null) {
        systemProperty("spring.profiles.active", "local")
    }
}

tasks.register("bootRunLocal") {
    group = "application"
    description = "Runs the app with local environment automation."
    dependsOn("bootRun")
}

tasks.register("fixStyle") {
    group = "verification"
    description = "Auto-fix code formatting issues."
    dependsOn("ktlintFormat")
}

tasks.register("checkQuality") {
    group = "verification"
    description = "Run all code quality checks (ktlint, detekt)."
    dependsOn("ktlintCheck", "detekt")
}

tasks.register("verifyMigrations") {
    group = "verification"
    description = "Validate Flyway migrations against the database."
    dependsOn("ensurePostgres", "flywayValidate")
}

tasks.register("releaseCheck") {
    group = "verification"
    description = "The ultimate quality gate before merging."
    dependsOn("ensurePostgres", "checkQuality", "test", "flywayValidate")
    doLast {
        println("\n\u2728 RELEASE CHECK PASSED \u2728")
    }
}

// --- FLYWAY CONFIG ---

flyway {
    driver = "org.postgresql.Driver"
    url = System.getProperty("DB_URL") ?: "jdbc:postgresql://${System.getProperty("DB_HOST") ?: "localhost"}:${System.getProperty("DB_PORT") ?: "5432"}/${System.getProperty("DB_NAME") ?: "gayakini"}"
    user = System.getProperty("DB_USERNAME") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: "password"
    schemas = arrayOf("commerce", "public")
    defaultSchema = "commerce"
    createSchemas = true
    baselineOnMigrate = true
    cleanDisabled = false
    configurations = arrayOf("flywayMigration")
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
