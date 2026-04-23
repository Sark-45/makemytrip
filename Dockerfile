# ─────────────────────────────────────────────────────────────────
#  Multi-stage Dockerfile — uses mvn directly (no Maven wrapper needed)
#  Stage 1: Build the fat JAR
#  Stage 2: Minimal JRE runtime
# ─────────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven directly — no wrapper needed
RUN apk add --no-cache maven

WORKDIR /build

# Copy pom.xml first so Docker caches the dependency download layer.
# Dependencies are only re-downloaded if pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR
COPY src/ src/
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the fat JAR from builder
COPY --from=builder /build/target/makemytrip-0.0.1-SNAPSHOT.jar app.jar

# Railway/Render inject PORT; Spring reads ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
