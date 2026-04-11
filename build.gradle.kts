import java.net.Socket
import org.flywaydb.core.Flyway
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf
import java.sql.DriverManager

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

val flywayMigration by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24")
    implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:1.1.24")
    flywayMigration("org.postgresql:postgresql:42.7.4")
    flywayMigration("org.flywaydb:flyway-core:10.20.1")
    flywayMigration("org.flywaydb:flyway-database-postgresql:10.20.1")
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.apache.tika:tika-core:2.9.2")
}

// --- MODULARIZED SCRIPTS ---
apply(from = "gradle/scripts/quality.gradle.kts")
apply(from = "gradle/scripts/local-env.gradle.kts")

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("${project.rootDir}/config/detekt/detekt.yml"))
    baseline = file("${project.rootDir}/config/detekt/baseline.xml")
    ignoreFailures = false
}

// --- DATABASE ORCHESTRATION ---

@Suppress("UNCHECKED_CAST")
fun getLocalEnv(): Map<String, String> {
    return project.extra.get("localEnv") as Map<String, String>
}

tasks.register("dbVerify") {
    group = "database"
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
    group = "database"
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
    group = "database"
    dependsOn("dbStart", "flywayMigrateLocal", "dbVerify", "dbSeed")
}

fun createLocalFlyway(
    pDir: String,
    env: Map<String, String>,
): Flyway {
    val host = env["DB_HOST"] ?: "localhost"
    val port = env["DB_PORT"] ?: "5432"
    val name = env["DB_NAME"] ?: "gayakini"
    val dbUrl = env["DB_URL"] ?: "jdbc:postgresql://$host:$port/$name"
    val user = env["DB_USERNAME"] ?: "postgres"
    val password = env["DB_PASSWORD"] ?: "password"

    return Flyway.configure()
        .dataSource(dbUrl, user, password)
        .schemas("commerce", "public")
        .defaultSchema("commerce")
        .baselineOnMigrate(true)
        .locations("filesystem:$pDir/src/main/resources/db/migration")
        .load()
}

tasks.register("flywayMigrateLocal") {
    group = "database"
    dependsOn("dbStart")
    val env = getLocalEnv()
    val pDir = project.rootDir.absolutePath
    doLast {
        createLocalFlyway(pDir, env).migrate()
        println("[✅] Flyway migrate complete.")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val env = getLocalEnv()
    doFirst { env.forEach { (k, v) -> environment(k, v) } }
    systemProperty("spring.profiles.active", "local")
}

tasks.register("releaseCheck") {
    group = "verification"
    dependsOn("clean", "qualityCheck", "assemble", "validateMcp")
}

tasks.register("validateMcp") {
    group = "verification"
    val pDir = project.rootDir.absolutePath
    val execOps = serviceOf<ExecOperations>()
    doLast {
        val cmd =
            "\u0024env:PROJECT_ROOT='$pDir'; " +
                "\u0024env:GITHUB_PERSONAL_ACCESS_TOKEN='dummy'; " +
                "Get-ChildItem 'tooling/mcp/start-*.ps1' | ForEach-Object { " +
                "& powershell.exe -NoProfile -ExecutionPolicy Bypass -File \u0024_.FullName -ValidateOnly }"
        execOps.exec {
            commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", cmd)
        }
    }
}

flyway {
    driver = "org.postgresql.Driver"
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
