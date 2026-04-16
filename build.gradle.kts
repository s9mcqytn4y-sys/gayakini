import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.0"
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
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:1.1.24")

    // Payment & Shipping SDKs
    implementation("com.midtrans:java-library:3.2.2")

    // Test Baseline
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

// --- TEST CONFIGURATION ---

tasks.withType<Test> {
    useJUnitPlatform {
        if (project.hasProperty("excludeIntegration")) {
            excludeTags("integration")
        }
    }
    testLogging {
        events("failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }

    // Pass system properties to the test JVM
    val tcEnabled =
        project.findProperty("testcontainers.enabled")?.toString()
            ?: System.getProperty("testcontainers.enabled")
            ?: System.getenv("TESTCONTAINERS_ENABLED")
            ?: "true"
    systemProperty("testcontainers.enabled", tcEnabled)

    // Clean summary for agents and CI
    @Suppress("MaxLineLength")
    afterSuite(
        KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) {
                val resultsText =
                    "Results: ${result.resultType} (${result.testCount} tests, " +
                        "${result.successfulTestCount} successes, ${result.failedTestCount} failures, " +
                        "${result.skippedTestCount} skipped)"
                val startItem = "|  "
                val endItem = "  |"
                val repeatLength = resultsText.length + startItem.length + endItem.length
                println("\n" + ("-".repeat(repeatLength)))
                println("$startItem$resultsText$endItem")
                println("-".repeat(repeatLength) + "\n")
            }
        }),
    )
}

// --- KOVER CONFIGURATION ---

kover {
    reports {
        verify {
            rule {
                // isEnabled = true // Deprecated/Removed in 0.8.x, enabled by default
                groupBy.set(kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION)
                bound {
                    minValue.set(35)
                }
            }
        }
        filters {
            excludes {
                classes(
                    "com.gayakini.GayakiniApplicationKt",
                    "**.*Dto",
                    "com.gayakini.infrastructure.config.*",
                    "com.gayakini.common.api.*",
                )
            }
        }
    }
}

// --- CI PIPELINE ORCHESTRATION ---

tasks.register("ciBuild") {
    group = "verification"
    description = "Runs all checks required for CI/CD pipeline"
    dependsOn("ktlintCheck", "detekt", "test", "koverVerify", "koverHtmlReport", "bootJar")
}

// Deterministic ordering for fail-fast behavior
tasks.named("detekt") { mustRunAfter("ktlintCheck") }
tasks.named("test") { mustRunAfter("detekt") }
tasks.named("koverVerify") { mustRunAfter("test") }
tasks.named("koverHtmlReport") { mustRunAfter("koverVerify") }
tasks.named("bootJar") { mustRunAfter("koverHtmlReport") }
