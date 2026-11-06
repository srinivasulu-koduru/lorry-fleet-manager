# ==============================================================================
# BHP Logistics Fleet Manager - Backend Dockerfile
# Multi-stage production build for Java 21 & Spring Boot 3
# Tuned for Voroa Free Nano (512 MB RAM limit)
# ==============================================================================

# --- Stage 1: Build the Application JAR ---
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Cache Maven wrapper and dependencies first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and compile production JAR
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Minimal Production Runtime ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl in runtime stage for reliable container healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create a non-root system user and group for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

# Create directory for file uploads and set permissions
RUN mkdir -p /app/uploads/documents && chown -R appuser:appgroup /app

# Copy the fat JAR from the builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

# JVM memory configuration for 512 MB RAM container:
# -XX:+UseContainerSupport: accurately detects cgroup container memory limits
# -XX:MaxRAMPercentage=75.0: caps heap at ~384 MB, reserving ~128 MB for Metaspace/threads
# -XX:InitialRAMPercentage=40.0: quick boot heap allocation (~200 MB)
# -XX:+ExitOnOutOfMemoryError: clean exit on OOM so container orchestrator restarts smoothly
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=40.0 -XX:+UseSerialGC -XX:CICompilerCount=2 -Djava.security.egd=file:/dev/./urandom -XX:+ExitOnOutOfMemoryError"
ENV PORT=8080

EXPOSE 8080

USER appuser

# Production container healthcheck using lightweight /api/health endpoint
# --start-period=90s accommodates Java 21 Spring Boot cold start and SSL connection in constrained containers
HEALTHCHECK --interval=10s --timeout=5s --start-period=180s --retries=10 \
  CMD curl -f http://localhost:${PORT:-8080}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
