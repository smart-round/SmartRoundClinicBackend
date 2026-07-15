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

Kotlin/Ktor REST API backend with 10 Gradle modules:

```
:common        — Shared DTOs, enums, constants, Resource<T>, DefaultResponse<T>, Permission enum, PolicyGroupPermissionResolver interface
:infra         — Ktor plugins, MongoDB client, Koin DI bootstrap, JWT, Cloudflare R2, authorization guards
:auth          — Reference implementation: full auth flow with JWT, user management
:admin         — Specialities, sub-specialities, service tiers, KMPDC practitioners, policy groups
:doctor        — Practitioner profiles, compliance, licences, certifications, payments, ratings, recommendations
:patient       — Patient profiles (stub)
:scheduling    — Appointments + doctor availability + slot engine
:notification  — Transactional email via Resend API (no REST endpoints)
:support       — Support tickets and chat
:consultation  — Real-time doctor-patient chat (WebSocket + MongoDB change stream), file sharing via R2
src/           — Root module: wires everything together, entry point
```

Entry point: `src/main/kotlin/Application.kt` → Netty on `0.0.0.0:8080`.

### Layer Rules

Each feature module follows four layers (package convention: `ke.co.smartroundclinic.{module}.{layer}`):

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
- IDs (`ObjectId().toString()`) and timestamps (`Clock.System.now().toString()`) are set in `toModel()` on request DTOs, not in entities

### Response Wrapper

All use cases return `DefaultResponse<T>`. The conversion chain:

1. Repository returns `Resource<T>` (sealed class: `Resource.Success` / `Resource.Error`)
2. Use case calls `.toDefaultResponse(statusCode) { transform }` to produce `DefaultResponse<T?>`  — note the return type is always `DefaultResponse<T?>` (nullable)
3. Controller calls `call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)`

`Resource<T>` and `DefaultResponse<T>` are defined in `:common`.

### Dependency Injection

Koin with named qualifiers for database instances:

```kotlin
get(named("authDb"))         // src_auth
get(named("adminDb"))        // src_admin
get(named("doctorDb"))       // src_doctor
get(named("patientDb"))      // src_patient
get(named("schedulingDb"))   // src_scheduling
get(named("supportDb"))      // src_support
get(named("consultationDb")) // src_consultation
```

All feature Koin modules are registered in `Application.kt` via `configureInfraModule(appModules = listOf(...))`. Validators are registered there too:

```kotlin
configureInfraModule(
    appModules = listOf(appConfigModule, databaseModule, ..., schedulingKoinModule),
    validators = { registerDoctorValidators(); registerAdminValidators() }
)
```

### Authentication & Authorization

JWT (HMAC256). Two token types:
- **Access token**: 24h expiry, carries `userId`, `role`, and `permissions` claims
- **Refresh token**: 30-day expiry

Four roles: `SUPER_ADMIN`, `ADMIN`, `DOCTOR`, `PATIENT`.

Authorization guards live in `infra/plugins/Security.kt`:

```kotlin
// Role guard — SUPER_ADMIN automatically satisfies any role check
call.requireRole("ADMIN") { ... }

// Permission guard — SUPER_ADMIN bypasses; ADMIN must hold all listed permissions
call.requirePermission(Permission.MANAGE_PATIENTS) { ... }

call.getRole()        // String? from JWT
call.getUserId()      // String? from JWT, responds 401 if missing
call.getPermissions() // List<Permission> from JWT permissions claim
```

#### RBAC for Admins

`SUPER_ADMIN` has unrestricted access. Regular `ADMIN` users are assigned to a **policy group** (managed in `:admin`). When an admin signs in, their policy group's permissions are resolved and embedded as a comma-joined `permissions` claim in the JWT. Permissions take effect on the next sign-in after assignment (max 24h lag).

The cross-module resolution path: `PolicyGroupPermissionResolver` interface lives in `:common`; `PolicyGroupRepositoryImpl` in `:admin` implements it; `UserRepositoryImpl` in `:auth` receives it via Koin's `getOrNull<PolicyGroupPermissionResolver>()` and calls it during `emailSignIn`.

The `PolicyGroupRepositoryImpl` holds both `adminDb` (for policy group documents) and `authDb` (for updating user `policyGroupId` field via raw `Document` updates — no type-safe `UserEntity` is imported in `:admin`).

OTPs expire in 15 minutes, hashed with JBCrypt.

### Request Validation

Uses Ktor's `RequestValidation` plugin. Each module defines a `fun RequestValidationConfig.registerXxxValidators()` extension. String fields must use `isNullOrBlank()` (not `isBlank()`) — Kotlinx serialization can pass `null` for non-null `String` fields at runtime.

### MongoDB

MongoDB Atlas (replica set required for transactions). Collection name constants in `common/MongoDBConstants.kt`. Schema-less — no migration tooling. On first run, `UserRepositoryImpl.initAdmin()` seeds `admin@smartroundclinic.co.ke` as `SUPER_ADMIN`.

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
`withTransaction {}` does NOT exist in the MongoDB Kotlin coroutine driver (5.6.4).

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
| POST | `/auth/user/create-admin` | SUPER_ADMIN |
| POST | `/auth/user/create-super-admin` | SUPER_ADMIN |
| GET | `/auth/user/account-verification?email=X&otpCode=Y` | None |
| GET | `/auth/user/account-verification/resend-otp?email=X` | None |
| PUT | `/auth/user` | JWT |
| GET | `/auth/user` | JWT |
| POST | `/auth/user/password-reset/request?email=X` | None |
| POST | `/auth/user/password-reset` | None |
| POST | `/auth/user/token/refresh` | None |
| DELETE | `/auth/user/token/revoke` | JWT |

### Admin — Policy Groups (`/admin/policy-groups`)
All endpoints require `SUPER_ADMIN`.

| Method | Path |
|--------|------|
| POST | `/admin/policy-groups` |
| GET | `/admin/policy-groups` |
| GET | `/admin/policy-groups/{id}` |
| PUT | `/admin/policy-groups/{id}` |
| DELETE | `/admin/policy-groups/{id}` |
| POST | `/admin/policy-groups/{id}/assign/{adminId}` |
| DELETE | `/admin/policy-groups/{id}/assign/{adminId}` |

Available permissions (defined in `common/Permission.kt`): `VIEW_PATIENTS`, `MANAGE_PATIENTS`, `VIEW_DOCTORS`, `MANAGE_DOCTORS`, `MANAGE_SPECIALITIES`, `VIEW_APPOINTMENTS`, `MANAGE_APPOINTMENTS`, `VIEW_REPORTS`, `MANAGE_ADMINS`.

### Scheduling (`/scheduling`)
| Method | Path | Notes |
|--------|------|-------|
| POST | `/scheduling/appointments` | PATIENT — body: `{doctorId, date, slotStart, notes?}` |
| GET | `/scheduling/appointments/{id}` | JWT |
| GET | `/scheduling/appointments/patient/{patientId}` | JWT |
| GET | `/scheduling/appointments/doctor/{doctorId}` | JWT |
| PATCH | `/scheduling/appointments/{id}/confirm\|cancel\|complete` | JWT |
| POST | `/scheduling/availability` | DOCTOR — create/replace full weekly schedule |
| GET | `/scheduling/availability/schedule` | DOCTOR/ADMIN — full weekly schedule config |
| GET | `/scheduling/availability?doctorId=X&date=YYYY-MM-DD` | DOCTOR/PATIENT — available slot start times |
| PUT/DELETE | `/scheduling/availability?day=0-6` | DOCTOR/ADMIN — update or deactivate a day |
| GET | `/scheduling/calendar?doctorId=X&view=day\|week\|month&date=X&forDoctor=bool` | DOCTOR/PATIENT — slot calendar |
| WS | `/scheduling/calendar/live?doctorId=X` | Live appointment change notifications |

#### Appointment Duration Model

Both `consultationDuration` and `gracePeriod` come from the **service tier** (stored in MongoDB as milliseconds; `ServiceTierLookup` divides by 60,000 on read to produce minutes).

`DoctorSchedule.slotDuration` = **calendar display grid step only** (used by `GetCalendarRangeUseCase` to render visual slot blocks). It does not drive booking logic.

Consultation duration and grace period are resolved at booking time via:
`doctorId → doctor_specializations.specializationId → admin_specialities.serviceTierId → admin_service_tiers.{consultationDuration, gracePeriod}`

- Patients do **not** pass `serviceTierId` — the system resolves it.
- Error if doctor has no specialization: `"Doctor is not yet configured for appointments."`
- Error if speciality has no `serviceTierId`: `"This speciality is not yet approved for offering appointments."`

```
appointmentPeriod   = consultationDuration + gracePeriod
slotEnd             = slotStart + consultationDuration + gracePeriod   (stored on AppointmentEntity)
slots generated at  = appointmentPeriod intervals
conflict detection  = [s, s+consultationDuration) vs [bs, be)
                      where be = bookedSlotEnd (already includes gracePeriod)
```

`AppointmentEntity` stores `serviceTierId` and `consultationDuration` (consultation only, not including grace period) for reference.

#### Slot Engine (`SlotEngine.kt`)

```kotlin
SlotEngine.computeAvailableSlots(
    schedule: DoctorSchedule,             // slotDuration = display grid step
    consultationDuration: Int,            // minutes, from ServiceTier
    bookedIntervals: List<Pair<Int,Int>>, // (slotStartMinutes, slotEndMinutes) — slotEnd includes gracePeriod
    overrides: List<SlotOverride>,
    nowMinutes: Int? = null,
    gridStep: Int? = null,                // defaults to consultationDuration + schedule.slotDuration
)
```

Conflict check: a candidate slot `s` is blocked if any booked interval `(bs, be)` satisfies `s < be && (s + consultationDuration) > bs`. No grace period is added in the check — it is already baked into the stored `be`.

Override types: `BLOCKED` (removes slots in range) | `EXTRA_AVAILABLE` (adds slots outside window).

#### Cross-module DB Access in Scheduling

`ServiceTierLookup` (scheduling data layer) holds raw `Document` collections from both `adminDb` and `doctorDb` — no Gradle dependency on `:admin` or `:doctor` modules.

`ConsultationInfoResult.Success(serviceTierId, consultationDuration, gracePeriod)` — all durations in minutes.

### Doctor (`/doctor`)

| Method | Path | Auth |
|--------|------|------|
| GET | `/doctor/recommendations?specializationId=X&page=1&size=20` | JWT |
| POST | `/doctor/ratings` | PATIENT |
| PUT | `/doctor/ratings/{id}` | PATIENT |
| DELETE | `/doctor/ratings/{id}` | PATIENT |
| GET | `/doctor/ratings?doctorId=X&page=1&size=20` | JWT |
| GET | `/doctor/ratings/{id}` | JWT |

Ratings require a completed appointment (`appointmentId` in request body); one rating per appointment.

Recommendation scoring: `rating×0.50 + bookings×0.35 + reviews×0.15` (log-normalized). General listing adds speciality popularity boost.

### Consultation (`/consultation`)

| Method | Path | Auth |
|--------|------|------|
| POST | `/consultation` | JWT — start or get session for a CONFIRMED/COMPLETED appointment (idempotent) |
| GET | `/consultation?id=X` or `?appointmentId=X` | JWT |
| PATCH | `/consultation/{id}/end` | DOCTOR |
| GET | `/consultation/{id}/messages?page=1&size=50` | JWT — paginated message history |
| WS | `/consultation/{id}/chat` | JWT — real-time chat (participants only) |

WebSocket frame format (send):
```json
{"type": "TEXT", "message": "Hello"}
{"type": "FILE", "fileName": "report.pdf", "contentType": "application/pdf", "fileData": "<base64>"}
```
Files are stored in R2 under `consultation-files/{consultationId}/{messageId}.{ext}` with 120-day presigned URLs. `videoRoomId` field on sessions reserved for future Cloudflare Realtime Kit integration.

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
JWT_REALM

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

# IntaSend (payments, payouts, wallets)
INTASEND_SECRET_KEY, INTASEND_BASE_URL (default: https://api.intasend.com/api/v1)
CALLBACK_URL, INTASEND_WEBHOOK_CHALLENGE
INTASEND_COLLECTIONS_WALLET_ID   # platform WORKING wallet that STK-push collections fund
INTASEND_COMMISSION_WALLET_ID    # platform's existing default SETTLEMENT wallet — commission lands here
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
