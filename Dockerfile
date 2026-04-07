# ---------- BUILD STAGE ----------
FROM gradle:8.11.1-jdk17-alpine AS builder

WORKDIR /app

# Copy config folder for Detekt baseline
COPY config config

# Copy source code
COPY src src
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Build executable jar using gradle directly
# We use app.jar as defined in build.gradle.kts bootJar task
RUN gradle clean bootJar -x test -x detekt -x ktlintCheck --no-daemon

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
