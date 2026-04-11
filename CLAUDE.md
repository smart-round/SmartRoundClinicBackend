# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the server locally (port 8080)
./gradlew run

# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build executable fat JAR
./gradlew buildFatJar

# Build Docker image
./gradlew buildImage

# Run via Docker
./gradlew runDocker
```

## Architecture

This is a **Kotlin/Ktor** REST API backend using **clean architecture** with 7 Gradle modules:

```
:common        — Shared DTOs, enums, constants, utilities
:infra         — Ktor plugins, MongoDB client, Koin DI setup, JWT config
:auth          — Full auth feature (only module with complete implementation)
:doctor        — Stub (skeleton only)
:patient       — Stub (skeleton only)
:admin         — Stub (skeleton only)
:notification  — Email sending via Resend API (no REST endpoints)
src/           — Root module: wires all modules together, entry point
```

Entry point: `src/main/kotlin/Application.kt` → starts Netty on `0.0.0.0:8080`.

### Layer Rules (enforced by module dependencies)

Each feature module (`:auth`, `:doctor`, etc.) follows three layers:

- **Domain** — use cases, repository interfaces, entities. No external deps.
- **Data** — repository implementations, MongoDB queries. Depends on Domain.
- **Presentation** — Ktor route handlers. Depends on Domain only (not Data).

All feature modules depend on `:infra` (for DI, DB, JWT) and `:common` (for shared types).

### Dependency Injection

Koin modules are defined per-layer in each feature module and registered in `:infra`. DI bindings follow: `DataModule` provides repository impls, `DomainModule` provides use cases, `PresentationModule` provides route-level objects.

### Authentication

JWT (HMAC256) with two token types:
- **Access token**: 24-hour expiry, carries `userId` and `role` claims
- **Refresh token**: 30-day expiry

Role-based access uses `call.requireRole()` helper. Three roles: `ADMIN`, `DOCTOR`, `PATIENT`.

### Database

MongoDB Atlas (cloud). Four logical databases: `src_auth`, `src_admin`, `src_doctor`, `src_patient`. Constants in `MongoDBConstants.kt` inside `:infra`. No migration tooling — schema-less. On first run, `UserRepositoryImpl.initAdmin()` seeds an admin account (`admin@smartroundclinic.co.ke`).

### Notification Module

Sends transactional emails through the Resend API using template IDs. Called asynchronously from auth use cases. Has no routes of its own.

### Auth API Endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/auth/user/sign-up?role=DOCTOR\|PATIENT` | None |
| POST | `/auth/user/sign-in` | None |
| POST | `/auth/user/create-admin` | ADMIN role |
| GET | `/auth/user/account-verification?email=X&otpCode=Y` | None |
| GET | `/auth/user/account-verification/resend-otp?email=X` | None |
| PUT | `/auth/user` | JWT |
| GET | `/auth/user` | JWT |

OTPs expire in 2 minutes. Passwords and OTPs are hashed with JBCrypt.

### Observability

- Prometheus metrics: `GET /metrics-micrometer`
- Health checks: `GET /health` (free memory ≥250MB, CPU ≤80%)

## Environment Variables

Required in `.env` (loaded at startup via `AppConfig.kt`):

```
# MongoDB
MONGODB_HOST, MONGODB_USER, MONGODB_PASSWORD

# JWT
JWT_SECRET, REFRESH_SECRET
JWT_AUDIENCE   # default: "smartroundclinic"
JWT_DOMAIN     # default: "smartroundclinic.co.ke"

# Resend (email)
RESEND_BASE_URL, RESEND_API_KEY
RESEND_ONBOARDING_EMAIL, RESEND_ONBOARDING_TEMPLATE_ID
RESEND_ACCOUNT_VERIFICATION_EMAIL, RESEND_ACCOUNT_VERIFICATION_TEMPLATE_ID
RESEND_DOCTOR_ACCOUNT_VERIFICATION_SUCCESS_TEMPLATE_ID
RESEND_PATIENT_ACCOUNT_VERIFICATION_SUCCESS_TEMPLATE_ID
RESEND_RESET_EMAIL
RESEND_PASSWORD_RESET_REQUEST_TEMPLATE_ID
RESEND_PASSWORD_RESET_CONFIRMATION_TEMPLATE_ID
```

## Key Libraries

Versions are centralized in `gradle/libs.versions.toml`.

- **Ktor 3.4.2** — HTTP server + client
- **Koin 4.1.2-Beta1** — Dependency injection
- **MongoDB Kotlin Driver 5.6.4** — Coroutine-based DB access
- **Auth0 JWT** — Token creation/verification (via Ktor plugin)
- **JBCrypt 0.4** — Password and OTP hashing
- **Resend SDK 4.13.0** — Transactional email
- **Micrometer + Cohort** — Metrics and health checks

## Adding a New Feature Module

Follow `:auth` as the reference implementation. Each new module needs:
1. A Gradle subproject directory with `build.gradle.kts` depending on `:infra` and `:common`
2. Domain, Data, and Presentation packages inside
3. Koin modules registered in `:infra`'s DI setup
4. Routes mounted in the root `Application.kt`
5. Entry in `settings.gradle.kts`

> Note: CORS is currently open to any host. Restrict before production deployment.