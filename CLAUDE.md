# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the server locally (port 8080)
./gradlew run

# Build all modules
./gradlew build

# Compile a single module (e.g. scheduling)
./gradlew :scheduling:compileKotlin

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

Kotlin/Ktor REST API backend with 8 Gradle modules:

```
:common        — Shared DTOs, enums, constants, Resource<T>, DefaultResponse<T>
:infra         — Ktor plugins, MongoDB client, Koin DI bootstrap, JWT, Cloudflare R2
:auth          — Reference implementation: full auth flow with JWT
:admin         — Specialities, sub-specialities, service tiers, KMPDC practitioners
:doctor        — Practitioner profiles, compliance, licences, certifications, payments
:patient       — Stub (skeleton only)
:scheduling    — Appointments + doctor availability
:notification  — Transactional email via Resend API (no REST endpoints)
src/           — Root module: wires everything together, entry point
```

Entry point: `src/main/kotlin/Application.kt` → Netty on `0.0.0.0:8080`.

### Layer Rules

Each feature module follows three layers (package convention: `ke.co.smartroundclinic.{module}.{layer}`):

- **Domain** — use cases, repository interfaces, domain models. No external deps.
- **Data** — repository implementations, entity classes, MongoDB queries.
- **Presentation** — Ktor route controllers, request/response DTOs.
- **Koin** — DI module (`val xxxModule = module { ... }`).

All feature modules depend on `:infra` and `:common`. Submodules must NOT have their own `settings.gradle.kts`.

### Data Flow

**Inbound**: `RequestDTO.toModel()` → domain model → `DomainModel.toEntity()` → MongoDB save

**Outbound**: MongoDB returns entity → `Entity.toModel()` → domain model → `DomainModel.toRes()` → response DTO

Rules:
- `toModel()` lives on request DTOs and entity classes
- `toEntity()` lives as an extension function in the entity file (on the domain model)
- `toRes()` lives as an extension function in the response file (on the domain model)
- Domain models in `domain/model/` must NOT import from `presentation/`
- IDs (`ObjectId().toString()`) and timestamps (`Instant.now()`) are set in `toModel()` on request DTOs, not in entities

### Response Wrapper

All use cases return `DefaultResponse<T>`. The conversion chain:

1. Repository returns `Resource<T>` (sealed class: `Resource.Success` / `Resource.Error`)
2. Use case calls `.toDefaultResponse { transform }` to produce `DefaultResponse<T>`
3. Controller calls `call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)`

`Resource<T>` and `DefaultResponse<T>` are defined in `:common`.

### Dependency Injection

Koin with named qualifiers for database instances:

```kotlin
get(named("authDb"))       // src_auth
get(named("adminDb"))      // src_admin
get(named("doctorDb"))     // src_doctor
get(named("patientDb"))    // src_patient
get(named("schedulingDb")) // src_scheduling
```

All feature Koin modules are registered in `Application.kt` via `configureInfraModule(appModules = listOf(...))`. Validators are registered there too:

```kotlin
configureInfraModule(
    appModules = listOf(appConfigModule, databaseModule, ..., schedulingKoinModule),
    validators = { registerDoctorValidators(); registerAdminValidators() }
)
```

### Authentication

JWT (HMAC256). Two token types:
- **Access token**: 24h expiry, carries `userId` and `role` claims
- **Refresh token**: 30-day expiry

Role-based access: `call.requireRole("ADMIN")`, `call.getRole()`, `call.getUserId()`. Three roles: `ADMIN`, `DOCTOR`, `PATIENT`.

### Request Validation

Uses Ktor's `RequestValidation` plugin. Each module defines a `fun RequestValidationConfig.registerXxxValidators()` extension. String fields must use `isNullOrBlank()` (not `isBlank()`) — Kotlinx serialization can pass `null` for non-null `String` fields at runtime.

### MongoDB

MongoDB Atlas (replica set required for transactions). Collection name constants in `common/MongoDBConstants.kt`. Schema-less — no migration tooling. On first run, `UserRepositoryImpl.initAdmin()` seeds `admin@smartroundclinic.co.ke`.

For multi-step operations requiring atomicity, use manual client session transactions:
```kotlin
val session = client.startSession()
try {
    session.startTransaction()
    // ... operations with session
    session.commitTransaction()
} catch (e: Exception) {
    runCatching { session.abortTransaction() }
} finally {
    session.close()
}
```
Note: `withTransaction {}` does NOT exist in the MongoDB Kotlin coroutine driver (5.6.4).

### Storage

Cloudflare R2 for file uploads (speciality/sub-speciality icons). `R2StorageRepository` is injected into use cases that handle uploads/removals.

### Notification Module

Resend API with template IDs. Called asynchronously from auth use cases. No routes.

## API Endpoints

### Auth (`/auth/user`)
| Method | Path | Auth |
|--------|------|------|
| POST | `/auth/user/sign-up?role=DOCTOR\|PATIENT` | None |
| POST | `/auth/user/sign-in` | None |
| POST | `/auth/user/create-admin` | ADMIN |
| GET | `/auth/user/account-verification?email=X&otpCode=Y` | None |
| GET | `/auth/user/account-verification/resend-otp?email=X` | None |
| PUT | `/auth/user` | JWT |
| GET | `/auth/user` | JWT |

OTPs expire in 2 minutes, hashed with JBCrypt.

### Scheduling (`/scheduling`)
| Method | Path |
|--------|------|
| POST | `/scheduling/appointments` |
| GET | `/scheduling/appointments/{id}` |
| GET | `/scheduling/appointments/patient/{patientId}` |
| GET | `/scheduling/appointments/doctor/{doctorId}` |
| PATCH | `/scheduling/appointments/{id}/confirm\|cancel\|complete\|no-show` |
| POST | `/scheduling/availability` |
| GET/PUT/DELETE | `/scheduling/availability/{doctorId}` |

### Observability
- Prometheus metrics: `GET /metrics-micrometer`
- Health check: `GET /health` (free memory ≥250MB, CPU ≤80%)

## Environment Variables

Required in `.env` (loaded by `AppConfig.kt` / `EnvLoader.kt`):

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

# Cloudflare R2 (file storage)
R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME, R2_PUBLIC_URL
```

## Key Libraries

Versions in `gradle/libs.versions.toml`.

| Library | Version | Purpose |
|---------|---------|---------|
| Ktor | 3.4.2 | HTTP server + client |
| Koin | 4.1.2-Beta1 | Dependency injection |
| MongoDB Kotlin Driver | 5.6.4 | Coroutine DB access |
| Auth0 JWT | — | Token creation/verification |
| JBCrypt | 0.4 | Password and OTP hashing |
| Resend SDK | 4.13.0 | Transactional email |
| Micrometer + Cohort | — | Metrics and health checks |

## Adding a New Feature Module

Follow `:auth` as the reference implementation:
1. Create a Gradle subproject with `build.gradle.kts` depending on `:infra` and `:common` (no `settings.gradle.kts`)
2. Add the module to `settings.gradle.kts`
3. Create `domain/`, `data/`, `presentation/`, and `koin/` packages
4. Register the Koin module in `Application.kt`'s `configureInfraModule(appModules = ...)` list
5. Register validators in `Application.kt`'s `validators = { ... }` block
6. Mount routes by calling `xxxModule()` from `Application.module()`
