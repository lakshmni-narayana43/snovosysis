# Multi-stage Docker build for LedgerLine (Java 21 + Spring Boot 3.4.5)

# Stage 1: Build application using Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package executable JAR
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight Alpine JRE 21 runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S ledgerline && adduser -S ledgerline -G ledgerline
USER ledgerline

# Copy packaged JAR
COPY --from=builder /build/target/ledgerline-backend-1.0.0.jar app.jar

# Render injects PORT dynamically (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

