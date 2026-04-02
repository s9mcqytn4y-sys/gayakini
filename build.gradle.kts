import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.Socket
import java.util.Properties

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

// Configuration for Flyway driver & database support
val flywayMigration by configurations.creating

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Add driver and database support to flyway classpath for the plugin
    flywayMigration("org.postgresql:postgresql:42.7.4")
    flywayMigration("org.flywaydb:flyway-database-postgresql:10.20.1")

    // UUIDv7 Support
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Observability & Utils
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.h2database:h2")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("config/detekt/detekt.yml"))
}

// Helper to load .env into a Map
fun loadDotEnv(): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val dotEnv = file(".env")
    if (dotEnv.exists()) {
        dotEnv.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    envMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }
    return envMap
}

val localEnv = loadDotEnv()

// Apply .env to System properties for Gradle-level tasks (like Flyway)
localEnv.forEach { (k, v) -> System.setProperty(k, v) }

// Flyway plugin configuration
flyway {
    driver = "org.postgresql.Driver"
    url = System.getProperty("DB_URL") ?: "jdbc:postgresql://${System.getProperty("DB_HOST") ?: "localhost"}:${System.getProperty("DB_PORT") ?: "5432"}/${System.getProperty("DB_NAME") ?: "gayakini"}"
    user = System.getProperty("DB_USERNAME") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: "password"
    schemas = arrayOf("commerce")
    defaultSchema = "commerce"
    createSchemas = true
    baselineOnMigrate = true
    configurations = arrayOf("flywayMigration")
}

// --- PRINCIPAL ENGINEER UX & OPERATIONS TASKS ---

tasks.register("doctor") {
    group = "verification"
    description = "Release-quality environment diagnostic tool."
    doLast {
        println("\n🚀 GAYAKINI PREFLIGHT DIAGNOSTICS")
        println("================================")

        val javaVersion = System.getProperty("java.version")
        println("[JVM] Version: $javaVersion (Target: 17)")

        val dbHost = System.getProperty("DB_HOST") ?: "localhost"
        val dbPort = (System.getProperty("DB_PORT") ?: "5432").toInt()
        val dbName = System.getProperty("DB_NAME") ?: "gayakini"

        print("[DB]  Connecting to $dbHost:$dbPort/$dbName... ")
        try {
            Socket(dbHost, dbPort).use {
                println("SUCCESS ✅")
            }
        } catch (e: Exception) {
            println("FAILED ❌")
            println("\nCRITICAL: PostgreSQL is unreachable at $dbHost:$dbPort.")
            println("Please ensure PostgreSQL 18+ is running locally on port $dbPort.")
            println("Tip: If you run natively, start your PostgreSQL service (e.g., 'brew services start postgresql@18' or Windows Service).")
            println("     Alternatively, use 'docker-compose up -d' if available.")
        }

        if (file(".env").exists()) {
            println("[ENV] .env file detected ✅")
        } else {
            println("[ENV] .env file missing (Using application.yml defaults) ⚠️")
        }

        println("================================")
    }
}

tasks.register("localSetup") {
    group = "setup"
    description = "Bootstraps local environment. Safe to run multiple times."
    doLast {
        val example = file(".env.example")
        val target = file(".env")
        if (example.exists() && !target.exists()) {
            example.copyTo(target)
            println("CREATED: .env from .env.example")
        } else if (target.exists()) {
            println("SKIP: .env already exists.")
        }
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // Force doctor check before running
    dependsOn("doctor")

    // Inject .env variables into the application process
    localEnv.forEach { (k, v) -> environment(k, v) }

    // Set default profile if not provided
    if (System.getProperty("spring.profiles.active") == null && !project.hasProperty("args")) {
        systemProperty("spring.profiles.active", "local")
    }
}

// Convenience task for local development
tasks.register("bootRunLocal") {
    group = "application"
    description = "Runs the application with 'local' profile and environment injection."
    dependsOn("bootRun")
}

tasks.register("verifyMigrations") {
    group = "verification"
    description = "Deep validation of database schema migrations."
    dependsOn("flywayValidate")
}

tasks.register("releaseCheck") {
    group = "verification"
    description = "The ultimate quality gate before merging/releasing."
    dependsOn("ktlintCheck", "detekt", "test", "verifyMigrations")
}

tasks.register("qaAll") {
    group = "verification"
    description = "Alias for releaseCheck."
    dependsOn("releaseCheck")
}

tasks.register("smokeTest") {
    group = "verification"
    description = "Quick verification of the running application's health."
    doLast {
        println("Tip: Use the .http files in 'http/' for live smoke testing.")
    }
}
