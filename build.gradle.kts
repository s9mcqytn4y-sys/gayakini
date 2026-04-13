import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
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

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Kotlin Support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database & Persistence (Compile-time / Runtime only)
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")

    // Utils
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    implementation("com.github.f4b6a3:uuid-creator:6.0.0")
    implementation("org.apache.tika:tika-core:2.9.2")

    // Security & JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Documentation & Metrics
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // PDF Generation
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24")
    implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:1.1.24")

    // Payment & Shipping SDKs
    implementation("com.midtrans:java-library:3.2.2")

    // Test Baseline
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

// --- CORE QUALITY GUARDRAILS ---

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

// Wire quality gates to check
tasks.named("check") {
    dependsOn("ktlintCheck", "detekt")
}

// qualityGate: Fast feedback loop for developers
tasks.register("qualityGate") {
    group = "verification"
    description = "Runs essential quality checks: ktlint, detekt, and tests."
    dependsOn("ktlintCheck", "detekt", "test")
}

// Release Check Task: Aggregates all quality gates and build artifacts
tasks.register("releaseCheck") {
    group = "verification"
    description = "Full project validation before release: qualityGate and bootJar."
    dependsOn("qualityGate", "bootJar")
}

// --- COMPILER CONFIGURATION ---

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// --- BOOT CONFIGURATION ---

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "local")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
