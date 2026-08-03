# Multi-stage build for optimal caching and performance
FROM maven:3.9-eclipse-temurin-21 AS dependencies

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY --from=dependencies /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src

RUN mvn clean package -B --no-transfer-progress -DskipTests

# Runtime stage. jammy (Ubuntu-based), not alpine - see ../WebScraper/Dockerfile for why some
# alpine base images lack an arm64 manifest and force slow QEMU emulation on Apple Silicon.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

# Install Playwright's browsers (+ their OS-level shared library deps via --with-deps) directly in
# THIS final image, not the Maven builder stage: --with-deps runs apt-get under the hood, and
# apt-installed packages from a different base image wouldn't carry over via COPY, so installing
# in the builder stage's different OS would leave the wrong (or missing) libraries here at
# runtime. The Spring Boot repackaged jar nests dependencies under BOOT-INF/, which can't be put
# on a plain `-cp` classpath directly - unpack it temporarily just to run Playwright's installer
# CLI class, then discard the unpacked copy (only app.jar is needed at runtime).
#
# Installs the full default browser set (chromium, firefox, webkit), not just chromium: even
# though PDFGenerator.kt only ever launches chromium, Playwright.create() itself validates the
# driver's whole default browser manifest on first use and silently downloads any browser it
# finds missing - confirmed by testing (a first `POST /generate` against a chromium-only image
# downloaded ~176MiB of Firefox+Webkit before serving the request). Installing all three up front
# keeps that check a no-op, so the image has no runtime network dependency.
RUN mkdir /tmp/unpacked && cd /tmp/unpacked && unzip -q /app/app.jar && \
    java -cp "/tmp/unpacked/BOOT-INF/classes:/tmp/unpacked/BOOT-INF/lib/*" \
      com.microsoft.playwright.CLI install --with-deps && \
    rm -rf /tmp/unpacked

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
