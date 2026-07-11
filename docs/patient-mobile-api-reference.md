# Patient Mobile App — API Integration Reference

This document is the authoritative guide for integrating the SmartRound Clinic backend into a patient-facing mobile app. All endpoints, request shapes, response shapes, error codes, and flow sequences needed for the patient experience are described here.

---

## Global Conventions

**Base URL:** configured per environment — all paths below are relative to it (e.g. `https://api.smartroundclinic.co.ke`).

**Auth header:** every protected endpoint requires:
```
Authorization: Bearer <accessToken>
```

**Standard response envelope** — every endpoint returns this shape:
```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Human-readable message",
  "data": { }
}
```

On error, `status` is `false` and `data` is `null`:
```json
{
  "httpStatusCode": 400,
  "status": false,
  "message": "Specific error reason",
  "data": null
}
```

Always check `status` (boolean), not just `httpStatusCode`, when deciding success vs. failure. The `message` field is safe to display to the user in most cases.

**Content-Type:** `application/json` for all JSON requests. Multipart for profile picture upload (see P5).

---

## Token Handling

| Token | Lifetime | Usage |
|-------|----------|-------|
| `accessToken` | 24 hours | `Authorization` header on every protected call |
| `refreshToken` | 30 days | Exchange for a new access token when expired |

Store both tokens securely (e.g. iOS Keychain / Android EncryptedSharedPreferences). On any `401` response, attempt a token refresh (A5) before retrying. If the refresh also fails, redirect the user to sign-in.

---

---

# SECTION A — Authentication

---

## A1. Sign Up

Registers a new patient account. The account starts unverified — an OTP is sent to the provided email automatically.

**`POST /auth/user/sign-up?role=PATIENT`**
**Auth:** None

### Query Parameters

| Param | Value |
|-------|-------|
| `role` | Must be `PATIENT` |

### Request

```json
{
  "fullName": "Jane Doe",
  "email": "jane.doe@example.com",
  "password": "SecurePassword123",
  "gender": "FEMALE",
  "phoneNumber": "+254712345678",
  "dateOfBirth": "1995-06-15"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `fullName` | String | Yes | |
| `email` | String | Yes | Must be unique |
| `password` | String | Yes | |
| `gender` | String | No | `MALE` / `FEMALE` / `NON_BINARY` / `OTHER`. Default `NON_BINARY` |
| `phoneNumber` | String | No | |
| `dateOfBirth` | String | No | `YYYY-MM-DD` |

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "User Created Successfully",
  "data": {
    "id": "69abc123def456789012abcd",
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "gender": "FEMALE",
    "role": "PATIENT",
    "accountStatus": "INACTIVE",
    "verificationStatus": "UNVERIFIED",
    "phoneNumber": "+254712345678",
    "dateOfBirth": "1995-06-15",
    "profilePicture": null,
    "kraPin": null,
    "createdAt": "2026-05-17T08:00:00.000000Z",
    "updatedAt": null,
    "personalInfo": null
  }
}
```

> After this call, navigate the user to the **OTP verification screen** (A2). Do not attempt sign-in yet.

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"User with email jane.doe@example.com already exists"` |

---

## A2. Verify Account (OTP)

The OTP is emailed after sign-up. The user enters it here to activate their account.

**`GET /auth/user/account-verification?email=X&otpCode=Y`**
**Auth:** None

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `email` | String | Yes |
| `otpCode` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Account verified successfully.",
  "data": {
    "id": "69abc123def456789012abcd",
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "gender": "FEMALE",
    "role": "PATIENT",
    "accountStatus": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "phoneNumber": "+254712345678",
    "dateOfBirth": "1995-06-15",
    "profilePicture": null,
    "kraPin": null,
    "createdAt": "2026-05-17T08:00:00.000000Z",
    "updatedAt": null,
    "personalInfo": null
  }
}
```

> After a successful verification, navigate the user to sign-in (A3). Do not auto-sign-in — they still need to authenticate to get tokens.

### Error Responses

| HTTP | Message | Action |
|------|---------|--------|
| 400 | `"Invalid OTP"` | Show error, offer resend |
| 400 | `"OTP has expired"` | Auto-trigger resend (A2a) |
| 404 | `"User not found"` | Edge case — re-check email |

---

## A2a. Resend Account Verification OTP

Sends a fresh OTP if the previous one expired or was not received.

**`GET /auth/user/account-verification/resend-otp?email=X`**
**Auth:** None

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `email` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "OTP sent successfully",
  "data": null
}
```

> Implement a cooldown on the UI side (e.g. 60 seconds) to prevent the user from spamming resend.

---

## A3. Sign In

Authenticates a verified patient and returns access + refresh tokens.

**`POST /auth/user/sign-in`**
**Auth:** None

### Request

```json
{
  "email": "jane.doe@example.com",
  "password": "SecurePassword123"
}
```

### Response `200` — success

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR...",
    "accountStatus": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "policyGroupIds": [],
    "permissions": []
  }
}
```

> Store `accessToken` and `refreshToken` securely. `policyGroupIds` and `permissions` are always empty for `PATIENT` — ignore them.

### Response `200` — unverified account

When `status` is `false` and `data` contains token metadata:
```json
{
  "httpStatusCode": 200,
  "status": false,
  "message": "Account not verified. Please check your email for the OTP code.",
  "data": {
    "accessToken": null,
    "refreshToken": null,
    "accountStatus": "INACTIVE",
    "verificationStatus": "UNVERIFIED",
    "policyGroupIds": [],
    "permissions": []
  }
}
```

> When `status` is `false` on a sign-in response, check `data.verificationStatus`. If `UNVERIFIED`, navigate to the OTP screen and trigger A2a to resend the code.

### Error Responses

| HTTP | Message |
|------|---------|
| 401 | `"Invalid email or password"` |

---

## A4. Refresh Token

Exchange a valid refresh token for a new access token. Call this when a protected request returns `401`.

**`POST /auth/user/token/refresh`**
**Auth:** None

### Request

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR..."
}
```

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR...",
    "accountStatus": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "policyGroupIds": [],
    "permissions": []
  }
}
```

> Replace both tokens in storage — a new `refreshToken` is always returned. Discard the old one.

### Error Responses

| HTTP | Message | Action |
|------|---------|--------|
| 401 | `"Invalid or expired refresh token"` | Force sign-in |

---

## A5. Sign Out (Revoke Token)

Invalidates the refresh token server-side. Call on explicit sign-out.

**`DELETE /auth/user/token/revoke`**
**Auth:** Bearer token required

### Request

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR..."
}
```

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Token revoked successfully",
  "data": null
}
```

> After this call, clear both tokens from local storage regardless of the response. The access token will expire naturally after 24 hours.

---

---

# SECTION B — Password Reset

---

## B1. Request Password Reset

Sends an OTP to the user's email to begin the reset flow.

**`POST /auth/user/password-reset/request?email=X`**
**Auth:** None

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `email` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Password reset OTP sent",
  "data": null
}
```

> Always show a success message even if the email is not in the system — this prevents user enumeration.

---

## B2. Resend Password Reset OTP

**`GET /auth/user/password-reset/resend-otp?email=X`**
**Auth:** None

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `email` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "OTP resent successfully",
  "data": null
}
```

---

## B3. Reset Password

Submit the OTP and new password to complete the reset.

**`POST /auth/user/password-reset`**
**Auth:** None

### Request

```json
{
  "email": "jane.doe@example.com",
  "otpCode": "482910",
  "newPassword": "NewSecurePassword456"
}
```

| Field | Type | Required |
|-------|------|----------|
| `email` | String | Yes |
| `otpCode` | String | Yes |
| `newPassword` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Password reset successful",
  "data": null
}
```

> After a successful reset, navigate to sign-in. Do not auto-sign-in.

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"Invalid OTP"` |
| 400 | `"OTP has expired"` |
| 404 | `"User not found"` |

---

---

# SECTION C — Profile Management

---

## C1. Get My Profile

Returns the authenticated patient's full profile.

**`GET /auth/user`**
**Auth:** Bearer token required

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "User found",
  "data": {
    "id": "69abc123def456789012abcd",
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "gender": "FEMALE",
    "role": "PATIENT",
    "accountStatus": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "kraPin": null,
    "phoneNumber": "+254712345678",
    "dateOfBirth": "1995-06-15",
    "profilePicture": "https://cdn.smartroundclinic.co.ke/profile/jane-avatar.jpg",
    "createdAt": "2026-05-17T08:00:00.000000Z",
    "updatedAt": "2026-05-17T09:30:00.000000Z",
    "personalInfo": null
  }
}
```

> `profilePicture` is a pre-signed URL valid for 24 hours. Do not cache it beyond a session.

---

## C2. Update My Profile

Updates one or more profile fields. All fields are optional — only send fields that changed.

**`PUT /auth/user`**
**Auth:** Bearer token required

### Request

```json
{
  "fullName": "Jane M. Doe",
  "email": "jane.new@example.com",
  "phoneNumber": "+254722000000",
  "gender": "FEMALE",
  "dateOfBirth": "1995-06-15"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `fullName` | String? | Trimmed, blank values ignored |
| `email` | String? | Must be unique across all users |
| `phoneNumber` | String? | |
| `gender` | String? | `MALE` / `FEMALE` / `NON_BINARY` / `OTHER` |
| `dateOfBirth` | String? | `YYYY-MM-DD` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "User updated successfully",
  "data": {
    "id": "69abc123def456789012abcd",
    "fullName": "Jane M. Doe",
    "email": "jane.new@example.com",
    "gender": "FEMALE",
    "role": "PATIENT",
    "accountStatus": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "kraPin": null,
    "phoneNumber": "+254722000000",
    "dateOfBirth": "1995-06-15",
    "profilePicture": null,
    "createdAt": "2026-05-17T08:00:00.000000Z",
    "updatedAt": "2026-05-17T10:00:00.000000Z",
    "personalInfo": null
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"Email already in use"` |
| 200 | `"No changes detected"` (status: true — not an error) |

---

## C3. Upload Profile Picture

Uploads a new profile picture. Send as `multipart/form-data` with the image file in a field named `file`.

**`POST /auth/user/profile-picture`**
**Auth:** Bearer token required
**Content-Type:** `multipart/form-data`

### Request

A single file part. Accepted MIME types: `image/jpeg`, `image/png`, `image/webp`.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Profile picture updated successfully",
  "data": null
}
```

> After upload, call C1 to get the fresh pre-signed URL to display.

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"No image file provided"` |
| 400 | `"Unsupported image type"` |

---

## C4. Remove Profile Picture

Deletes the current profile picture.

**`DELETE /auth/user/profile-picture`**
**Auth:** Bearer token required

No request body.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Profile picture updated successfully",
  "data": null
}
```

---

---

# SECTION D — Availability (Booking Preparation)

---

## D1. Get Available Slots for a Date

Returns available `"HH:mm"` slot times for a specific doctor on a specific date. Use this to populate the time picker after the patient selects a date on the calendar.

**`GET /scheduling/availability?doctorId=<id>&date=2026-05-20`**
**Auth:** Bearer token required

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `doctorId` | String | Yes | Doctor's user ID |
| `date` | String | Yes | `YYYY-MM-DD` |

### Response `200` — slots available

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Available slots retrieved",
  "data": ["09:00", "09:30", "10:00", "10:30", "11:00", "14:00", "14:30", "15:00"]
}
```

### Response `200` — no slots

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Doctor not available on this day",
  "data": []
}
```

When `data` is an empty array, **disable the booking flow** for that date. Use the `message` field to choose the right UI copy:

| `message` | Display text |
|-----------|-------------|
| `"No availability for this day"` | "Doctor is not available on this day" |
| `"Doctor not available on this day"` | "Doctor is not available on this day" |
| `"Available slots retrieved"` with `data: []` | "No remaining slots for this date" |

---

## D2. Calendar View

Returns a day-by-day breakdown for a date range. Use this to highlight bookable days on a calendar before the patient picks a date.

**`GET /scheduling/calendar?doctorId=<id>&view=month&date=2026-05-01&forDoctor=false`**
**Auth:** Bearer token required

### Query Parameters

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `doctorId` | String | Yes | — | Doctor's user ID |
| `view` | String | No | `month` | `day` / `week` / `month` |
| `date` | String | No | Today | Any date within the desired range (`YYYY-MM-DD`) |
| `forDoctor` | Boolean | No | `false` | Always pass `false` for patient views |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Calendar retrieved successfully",
  "data": {
    "doctorId": "69f8846c319d59e154fdab3c",
    "days": [
      {
        "date": "2026-05-18",
        "dayOfWeek": "MONDAY",
        "isWorkingDay": true,
        "slots": [
          {
            "slotStart": "09:00",
            "slotEnd": "09:30",
            "status": "AVAILABLE",
            "appointmentId": null,
            "patientId": null
          }
        ]
      },
      {
        "date": "2026-05-19",
        "dayOfWeek": "TUESDAY",
        "isWorkingDay": false,
        "slots": []
      }
    ]
  }
}
```

### Rendering rules

| `isWorkingDay` | `slots` | How to render the date |
|----------------|---------|------------------------|
| `false` | `[]` | Greyed out — not bookable |
| `true` | non-empty | Highlighted — tap to see slots |
| `true` | `[]` | Greyed out (past slots for today) — not bookable |

> With `forDoctor=false`, only `AVAILABLE` slots are returned. `appointmentId` and `patientId` are always `null`.

---

---

# SECTION E — Appointments

---

## E1. Book an Appointment

**`POST /scheduling/appointments`**
**Auth:** Bearer token required (PATIENT)

### Request

```json
{
  "doctorId": "69f8846c319d59e154fdab3c",
  "date": "2026-05-20",
  "slotStart": "09:30",
  "notes": "Persistent headache for 3 days"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `doctorId` | String | Yes | |
| `date` | String | Yes | `YYYY-MM-DD` |
| `slotStart` | String | Yes | `HH:mm` — must be a value from D1's slot list |
| `notes` | String | No | Optional patient notes visible to the doctor |

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "Appointment booked successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "date": "2026-05-20",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "BOOKED",
    "bookedAt": "2026-05-17T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": null
  }
}
```

### Error Responses

| HTTP | Message | Action |
|------|---------|--------|
| 400 | `"Doctor has no schedule for this day"` | Refresh calendar — day removed |
| 400 | `"Doctor is not available on this day"` | Refresh calendar — day deactivated |
| 400 | `"Slot 09:30 is not available"` | Slot taken or past — refresh D1 |
| 409 | `"This slot has already been booked"` | Race condition — refresh D1 |

> **Always fetch fresh slots (D1) immediately before showing the booking confirmation screen.** On 400 or 409, re-call D1 and show the updated list.

---

## E2. Get My Appointments

Returns all appointments for the authenticated patient across all statuses.

**`GET /scheduling/appointments/patient`**
**Auth:** Bearer token required (PATIENT)

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Appointments retrieved successfully",
  "data": [
    {
      "id": "6a1234abcd5678ef90123456",
      "doctorId": "69f8846c319d59e154fdab3c",
      "patientId": "69abc123def456789012abcd",
      "date": "2026-05-20",
      "slotStart": "09:30",
      "slotEnd": "10:00",
      "status": "CONFIRMED",
      "bookedAt": "2026-05-17T08:30:00.123456Z",
      "notes": "Persistent headache for 3 days",
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": "2026-05-17T09:00:00.000000Z"
    },
    {
      "id": "6a5678abcd1234ef90456789",
      "doctorId": "69f8846c319d59e154fdab3c",
      "patientId": "69abc123def456789012abcd",
      "date": "2026-04-20",
      "slotStart": "10:00",
      "slotEnd": "10:30",
      "status": "COMPLETED",
      "bookedAt": "2026-04-15T10:00:00.000000Z",
      "notes": null,
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": "2026-04-20T10:35:00.000000Z"
    }
  ]
}
```

> Filter by `status` on the client side to populate upcoming, past, and cancelled tabs.

---

## E3. Get Single Appointment

**`GET /scheduling/appointments?id=<appointmentId>`**
**Auth:** Bearer token required (PATIENT or DOCTOR)

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Appointment retrieved successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "date": "2026-05-20",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "BOOKED",
    "bookedAt": "2026-05-17T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": null,
    "refund": null
  }
}
```

`refund` is only populated when `status` is `"CANCELLED"` **and** a completed payment existed for the appointment at cancellation time (free/unpaid cancellations have no refund record, so `refund` stays `null`). Shape:

```json
{
  "refund": {
    "id": "6a9988abcd5678ef90123abc",
    "amount": 1500.0,
    "currency": "KES",
    "status": "PENDING",
    "reason": "I am feeling better",
    "createdAt": "2026-05-18T10:15:00.123456Z",
    "updatedAt": null
  }
}
```

`refund.status` lifecycle: `PENDING` (record created on cancellation) → `COMPLETED` | `FAILED` (set once the payout is processed via IntaSend).

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Appointment not found"` |

---

## E4. Cancel an Appointment

A patient can cancel a `BOOKED` or `CONFIRMED` appointment.

**`PATCH /scheduling/appointments/cancel?id=<appointmentId>`**
**Auth:** Bearer token required (PATIENT)

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Request

```json
{
  "reason": "I am feeling better"
}
```

`reason` is optional. Send `{}` if no reason.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Appointment updated successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "date": "2026-05-20",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "CANCELLED",
    "bookedAt": "2026-05-17T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": "I am feeling better",
    "cancelledBy": "69abc123def456789012abcd",
    "updatedAt": "2026-05-18T07:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Appointment not found"` |
| 403 | `"Not authorized to cancel this appointment"` |

---

---

# SECTION F — Appointment Status Reference

## Status Lifecycle

```
BOOKED ──────────────────────► CANCELLED  (patient or doctor)
  │
  └─► CONFIRMED ──────────────► CANCELLED  (patient or doctor)
        │
        ├─► COMPLETED
        └─► NO_SHOW
```

## Status Meanings

| Status | Meaning | Patient action |
|--------|---------|----------------|
| `BOOKED` | Booking created, awaiting doctor confirmation | Can cancel |
| `CONFIRMED` | Doctor acknowledged the booking | Can cancel |
| `COMPLETED` | Visit took place | Read-only |
| `CANCELLED` | Cancelled before completion | Read-only |
| `NO_SHOW` | Patient did not attend | Read-only |

## `cancelledBy` field

When `status` is `CANCELLED`, compare `cancelledBy` to `patientId`:
- Match → patient cancelled
- No match → doctor cancelled — surface a different UI message (e.g. "Your doctor cancelled this appointment")

---

---

# SECTION G — Complete Flow Reference

## G1. Registration & Onboarding

```
1. POST /auth/user/sign-up?role=PATIENT          → account created (INACTIVE/UNVERIFIED)
2. Show OTP entry screen
3. GET  /auth/user/account-verification?email=X&otpCode=Y  → account activated
4. Navigate to sign-in
5. POST /auth/user/sign-in                        → get accessToken + refreshToken
6. GET  /auth/user                                → load profile for home screen
```

## G2. Returning User Sign-In

```
1. POST /auth/user/sign-in
   → status: true  → store tokens, proceed to home
   → status: false, verificationStatus: UNVERIFIED → navigate to OTP screen, call A2a
   → 401           → show "Invalid email or password"
```

## G3. Token Refresh (Transparent)

```
On any 401 from a protected endpoint:
1. POST /auth/user/token/refresh  { refreshToken }
   → 200  → replace both tokens, retry original request
   → 401  → clear tokens, navigate to sign-in
```

## G4. Book an Appointment

```
1. Patient selects a doctor
2. GET /scheduling/calendar?doctorId=X&view=month&date=YYYY-MM-01&forDoctor=false
   → highlight days where isWorkingDay: true AND slots is non-empty
3. Patient taps a highlighted date
4. GET /scheduling/availability?doctorId=X&date=YYYY-MM-DD
   → show available time slots as a picker
5. Patient selects a time and taps "Confirm Booking"
6. GET /scheduling/availability?doctorId=X&date=YYYY-MM-DD   ← re-fetch right before submitting
7. POST /scheduling/appointments  { doctorId, date, slotStart, notes }
   → 201  → show booking confirmation screen
   → 400 / 409 → refresh slots (step 6), prompt re-selection with updated list
```

## G5. My Appointments Screen

```
1. GET /scheduling/appointments/patient
   → client-side filter:
     Upcoming tab: status IN [BOOKED, CONFIRMED] AND date >= today
     Past tab:     status IN [COMPLETED, NO_SHOW, CANCELLED] OR date < today
2. Tap appointment → GET /scheduling/appointments?id=X
3. Cancel button (only if status is BOOKED or CONFIRMED):
   PATCH /scheduling/appointments/cancel?id=X  { "reason": "..." }
   → refresh appointment list
```

## G6. Password Reset

```
1. POST /auth/user/password-reset/request?email=X
2. Show OTP entry + new password fields
3. POST /auth/user/password-reset  { email, otpCode, newPassword }
   → 200  → navigate to sign-in
   → 400 "OTP has expired" → offer resend via GET /auth/user/password-reset/resend-otp?email=X
```

## G7. Rate a Doctor (After a Completed Appointment)

```
1. On the "Past" appointments tab, for any appointment with status COMPLETED:
   → check whether a rating already exists (GET /doctor/ratings?id=X if you stored the ratingId,
     or track locally that this appointmentId was already rated after step 3 succeeds)
2. Show a "Rate this doctor" prompt/star picker
3. POST /doctor/ratings  { appointmentId, doctorId, rating, comment }
   → 201  → show confirmation, hide the prompt for this appointment going forward
   → 409  → surface the specific message (e.g. "You have already rated this appointment")
4. Patient can edit later from their own ratings list:
   PUT /doctor/ratings?id=X  { rating, comment }
   DELETE /doctor/ratings?id=X
```

---

---

# SECTION H — Validation Rules Summary

| Field | Rule |
|-------|------|
| `role` at sign-up | Must be `PATIENT` (query param) |
| `gender` | `MALE` / `FEMALE` / `NON_BINARY` / `OTHER` |
| `dateOfBirth` | `YYYY-MM-DD` format |
| `date` (appointments / availability) | `YYYY-MM-DD` format |
| `slotStart` | `HH:mm` 24-hour format |
| `slotStart` at booking | Must be present in the slot list returned by D1 |
| `doctorId` | Required when booking |
| `rating` (doctor rating) | Integer, `1`–`5` |
| `appointmentId` (doctor rating) | Required — must reference a `COMPLETED` appointment owned by the patient |

---

# SECTION I — Common Mistakes to Avoid

1. **Using an expired pre-signed profile picture URL.** Always re-fetch from C1 when displaying a profile photo — do not persist the URL across sessions.

2. **Not re-fetching slots before booking.** Another patient may grab the slot between the picker screen and the confirmation screen. Always call D1 again immediately before `POST /scheduling/appointments`.

3. **Treating `status: true` on sign-in as "done".** Check `data.verificationStatus`. An unverified account returns `status: false` with a `data` object — never `null`.

4. **Caching both tokens forever.** `accessToken` is valid for 24h, `refreshToken` for 30 days. Implement the transparent refresh flow (G3) — do not re-prompt for credentials on every `401`.

5. **Filtering appointments server-side.** The patient appointments endpoint (`GET /scheduling/appointments/patient`) returns all statuses. Apply upcoming/past/cancelled filtering on the client.

6. **Sending all fields on profile update.** Only send fields that the user actually changed. The server skips fields with no diff, but sending `null` for optional fields has no effect.

7. **Treating all `POST /doctor/ratings` failures as generic errors.** Every business-rule rejection (appointment not found, not yours, wrong doctor, not completed yet, already rated) comes back as HTTP `409`, not `400` or `404`. Branch on `message`, not just the status code, to show the right copy.

8. **Assuming `DELETE /doctor/ratings?id=X` failing silently means it worked.** The endpoint returns `200`/`status: true` even if `id` didn't match a rating owned by the caller (it reports the deleted-count internally but always returns a generic success message). Re-fetch the ratings list after delete to confirm, rather than trusting the response alone.

---

---

# SECTION J — Rate a Doctor

A patient can rate a doctor once per completed appointment. Ratings feed into that doctor's `averageRating` / `totalReviews` (shown in profile and recommendations) and into recommendation scoring.

**Preconditions enforced server-side** (all checked on every submit):
- The appointment referenced by `appointmentId` must exist.
- It must belong to the calling patient (`patientId` from the JWT, not request body).
- It must be with the specified `doctorId`.
- Its `status` must be `COMPLETED`.
- No existing rating for that `appointmentId` (one rating per appointment, not per patient/doctor pair).

---

## J1. Submit a Rating

**`POST /doctor/ratings`**
**Auth:** Bearer token required (PATIENT)

### Request

```json
{
  "appointmentId": "6a5678abcd1234ef90456789",
  "doctorId": "69f8846c319d59e154fdab3c",
  "rating": 5,
  "comment": "Very thorough and patient, explained everything clearly."
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `appointmentId` | String | Yes | Must reference a `COMPLETED` appointment owned by this patient |
| `doctorId` | String | Yes | Must match the appointment's doctor |
| `rating` | Int | Yes | `1`–`5` |
| `comment` | String | No | Free text |

`patientId` is derived from the access token — do not send it.

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "Rating submitted successfully",
  "data": {
    "id": "6b1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 5,
    "comment": "Very thorough and patient, explained everything clearly.",
    "createdAt": "2026-05-20T11:00:00.000000Z",
    "updatedAt": null
  }
}
```

### Error Responses

All business-rule rejections return **`409`** (not 400/404) — always branch on `message`:

| HTTP | Message |
|------|---------|
| 409 | `"Appointment not found"` |
| 409 | `"This appointment does not belong to you"` |
| 409 | `"This appointment is not with the specified doctor"` |
| 409 | `"You can only rate a doctor after a completed appointment"` |
| 409 | `"You have already rated this appointment"` |
| 400 | Validation error — `rating` not in `1..5`, or `appointmentId`/`doctorId` missing |

---

## J2. Update My Rating

**`PUT /doctor/ratings?id=<ratingId>`**
**Auth:** Bearer token required (PATIENT — must own the rating)

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Request

```json
{
  "rating": 4,
  "comment": "Updating after a follow-up visit."
}
```

Both fields are optional individually, but **at least one is required** — sending both `null` is rejected client-request-validation-side with `400`. Only the fields you send are changed.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Rating updated successfully",
  "data": {
    "id": "6b1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 4,
    "comment": "Updating after a follow-up visit.",
    "createdAt": "2026-05-20T11:00:00.000000Z",
    "updatedAt": "2026-05-21T09:15:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Rating not found"` — wrong `id`, or the rating belongs to a different patient |
| 400 | Validation error — `rating` not in `1..5`, or both fields omitted |

---

## J3. Delete My Rating

**`DELETE /doctor/ratings?id=<ratingId>`**
**Auth:** Bearer token required (PATIENT — must own the rating)

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Rating deleted successfully",
  "data": null
}
```

> **Note:** this always returns `200`/`status: true`, even if `id` doesn't match any rating owned by the caller. There is no `404` on delete — if you need to confirm the deletion happened, re-fetch the rating (J4) or the list (J5) afterward.

---

## J4. Get a Single Rating

**`GET /doctor/ratings?id=<ratingId>`**
**Auth:** Bearer token required (any role)

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Success",
  "data": {
    "id": "6b1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 4,
    "comment": "Updating after a follow-up visit.",
    "createdAt": "2026-05-20T11:00:00.000000Z",
    "updatedAt": "2026-05-21T09:15:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Rating not found"` |

---

## J5. Get a Doctor's Ratings (Paginated)

Use this to render a doctor's review list on their profile screen.

**`GET /doctor/ratings?doctorId=<doctorId>&page=1&size=20`**
**Auth:** Bearer token required (any role)

### Query Parameters

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `doctorId` | String | Yes | |
| `page` | Int | No | Default `1` |
| `size` | Int | No | Default `20`, capped at `100` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Success",
  "data": {
    "items": [
      {
        "id": "6b1234abcd5678ef90123456",
        "appointmentId": "6a5678abcd1234ef90456789",
        "doctorId": "69f8846c319d59e154fdab3c",
        "patientId": "69abc123def456789012abcd",
        "rating": 4,
        "comment": "Updating after a follow-up visit.",
        "createdAt": "2026-05-20T11:00:00.000000Z",
        "updatedAt": "2026-05-21T09:15:00.000000Z"
      }
    ],
    "total": 37,
    "page": 1,
    "size": 20
  }
}
```

> The doctor's `averageRating` / `totalReviews` shown elsewhere (e.g. recommendations, profile) are recomputed automatically after every submit/update/delete — you don't need to compute them client-side, just display `data.averageRating` from the doctor profile endpoint.

---

## J6. Complete Flow — Rate a Doctor

```
1. Past appointments tab → appointment with status COMPLETED and not yet rated
2. Show star picker + optional comment field
3. POST /doctor/ratings  { appointmentId, doctorId, rating, comment }
   → 201  → mark this appointment as "rated" locally, show confirmation
   → 409 "You have already rated this appointment"  → treat as already-rated, hide the prompt
   → 409 (other messages)  → surface message, likely a stale local appointment cache — refresh E2
4. "My Reviews" screen → GET /doctor/ratings?doctorId=X or store the returned ratingId per appointment
   → Edit: PUT /doctor/ratings?id=X  { rating, comment }
   → Delete: DELETE /doctor/ratings?id=X, then re-fetch to confirm
```
