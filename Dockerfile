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

# Build executable jar using the installed Gradle in the image.
# We cap JVM memory for build stability in RAM-constrained CI environments.
RUN gradle clean ciBuild -PexcludeIntegration \
    -Dorg.gradle.jvmargs="-Xmx1536m -XX:+ExitOnOutOfMemoryError" \
    --no-daemon

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="Gayakini Dev Team <dev@gayakini.com>"
LABEL org.opencontainers.image.title="Gayakini Backend"
LABEL org.opencontainers.image.description="Industrial Supplier E-commerce Backend"

# Security: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Ensure correct permissions
COPY --from=builder --chown=spring:spring /app/build/libs/app.jar app.jar

USER spring:spring

# Performance: JVM optimizations for containers
# InitialRAMPercentage=25, MaxRAMPercentage=75, G1GC for low latency
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:NativeMemoryTracking=summary -XshowSettings:vm -Djava.security.egd=file:/dev/./urandom"

# Expose port
EXPOSE 8080

# Health Check: Ensure the application is up and responding
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep UP || exit 1

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
