# ---------- BUILD STAGE ----------
FROM gradle:8.11.1-jdk17-alpine AS builder

WORKDIR /app

# Copy gradle wrapper first for better caching
COPY gradlew .
COPY gradle gradle

# Copy config folder for Detekt baseline
COPY config config

# Copy source code and other files
COPY src src
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Build executable jar using the wrapper to ensure version consistency
# We use ciBuild task to ensure all quality gates (tests, coverage, lint, detekt) are passed
# We skip integration tests to avoid requiring a running database in the build environment
RUN chmod +x gradlew && ./gradlew clean ciBuild -PexcludeIntegration --no-daemon

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre-alpine

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
RUN chown -R spring:spring /app
USER spring:spring

# Copy built artifact - carefully select the bootable jar
COPY --from=builder /app/build/libs/app.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
