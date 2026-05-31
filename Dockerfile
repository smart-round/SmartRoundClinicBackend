# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# Copy dependency descriptors first so the layer is cached between source changes
COPY settings.gradle.kts gradle.properties* ./
COPY gradle/ gradle/
COPY build.gradle.kts ./

# Copy all sub-module build files
COPY common/build.gradle.kts          common/
COPY infra/build.gradle.kts           infra/
COPY auth/build.gradle.kts            auth/
COPY notification/build.gradle.kts    notification/
COPY doctor/build.gradle.kts          doctor/
COPY patient/build.gradle.kts         patient/
COPY admin/build.gradle.kts           admin/
COPY scheduling/build.gradle.kts      scheduling/
COPY support/build.gradle.kts         support/
COPY article/build.gradle.kts         article/
COPY consultation/build.gradle.kts    consultation/
COPY payments/build.gradle.kts        payments/

# Download dependencies (cached as a separate layer)
RUN gradle dependencies --no-daemon 2>/dev/null || true

# Copy source and build the fat JAR
COPY . .
RUN gradle buildFatJar --no-daemon

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user to run the process
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/smartroundclinic-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
