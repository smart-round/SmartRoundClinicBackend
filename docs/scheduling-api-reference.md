# Scheduling & Appointments — Mobile API Reference

All endpoints require `Authorization: Bearer <accessToken>` in the request header.  
All timestamps are ISO-8601 UTC strings. All times are `"HH:mm"` 24-hour format in the doctor's configured timezone.  
All responses are wrapped in the standard envelope:

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "...",
  "data": { }
}
```

On error, `status` is `false` and `data` is `null`:

```json
{
  "httpStatusCode": 400,
  "status": false,
  "message": "Slot 09:30 is not available",
  "data": null
}
```

---

## Day-of-Week Reference

| Value | Day |
|-------|-----|
| `0` | Monday |
| `1` | Tuesday |
| `2` | Wednesday |
| `3` | Thursday |
| `4` | Friday |
| `5` | Saturday |
| `6` | Sunday |

---

---

# SECTION A — Schedule Management (DOCTOR)

---

## A1. Create / Update a Day's Schedule

Creates a schedule for a day of the week. Calling this again for the same `dayOfWeek` **replaces** the existing entry (upsert — safe to call repeatedly).

**`POST /scheduling/availability`**  
**Role:** `DOCTOR`

### Request

```json
{
  "dayOfWeek": 0,
  "windowStart": "09:00",
  "windowEnd": "17:00",
  "slotDuration": 30,
  "breakBlocks": [
    { "start": "12:00", "end": "14:00" }
  ],
  "isActive": true,
  "timezone": "Africa/Nairobi"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `dayOfWeek` | Int | Yes | 0–6 |
| `windowStart` | String | Yes | `HH:mm` format |
| `windowEnd` | String | Yes | `HH:mm` format, must be after `windowStart` |
| `slotDuration` | Int | Yes | `25` or `30` only |
| `breakBlocks` | Array | No | Default `[]`. Each block: `{ "start": "HH:mm", "end": "HH:mm" }` |
| `isActive` | Boolean | No | Default `true`. Set `false` to block the entire day |
| `timezone` | String | No | IANA timezone. Default `"Africa/Nairobi"` |

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "Schedule saved successfully",
  "data": {
    "id": "6a008588ecae04bb606fd0ca",
    "doctorId": "69f8846c319d59e154fdab3c",
    "dayOfWeek": 0,
    "windowStart": "09:00",
    "windowEnd": "17:00",
    "slotDuration": 30,
    "breakBlocks": [
      { "start": "12:00", "end": "14:00" }
    ],
    "isActive": true,
    "timezone": "Africa/Nairobi",
    "createdAt": "2026-05-10T13:18:00.242234Z",
    "updatedAt": null
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"dayOfWeek must be 0-6 (0=Monday, 6=Sunday)"` |
| 400 | `"windowStart must be in HH:mm format"` |
| 400 | `"windowEnd must be after windowStart"` |
| 400 | `"slotDuration must be 25 or 30"` |
| 401 | Unauthorized |

---

## A2. Get Own Weekly Schedule

Returns all days the authenticated doctor has configured.

**`GET /scheduling/availability/schedule`**  
**Role:** `DOCTOR`

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Schedule retrieved successfully",
  "data": [
    {
      "id": "6a008588ecae04bb606fd0ca",
      "doctorId": "69f8846c319d59e154fdab3c",
      "dayOfWeek": 0,
      "windowStart": "09:00",
      "windowEnd": "17:00",
      "slotDuration": 30,
      "breakBlocks": [
        { "start": "12:00", "end": "14:00" }
      ],
      "isActive": true,
      "timezone": "Africa/Nairobi",
      "createdAt": "2026-05-10T13:18:00.242234Z",
      "updatedAt": "2026-05-10T14:08:35.706549Z"
    },
    {
      "id": "6a008658ecae04bb606fd0cb",
      "doctorId": "69f8846c319d59e154fdab3c",
      "dayOfWeek": 4,
      "windowStart": "09:00",
      "windowEnd": "17:00",
      "slotDuration": 30,
      "breakBlocks": [
        { "start": "12:00", "end": "14:00" }
      ],
      "isActive": false,
      "timezone": "Africa/Nairobi",
      "createdAt": "2026-05-10T13:21:28.506301Z",
      "updatedAt": "2026-05-10T14:07:10.191717Z"
    }
  ]
}
```

> Only configured days are returned. Days the doctor has never set up will not appear. A missing day is treated as unavailable by the slot engine.

---

## A3. Update a Single Day

Partially updates one day of the schedule. Only send fields that need to change — all fields are optional.

**`PUT /scheduling/availability?day=0`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `day` | Int (0–6) | Yes | Day to update |

### Request

```json
{
  "windowStart": "08:00",
  "windowEnd": "16:00",
  "slotDuration": 25,
  "breakBlocks": [
    { "start": "13:00", "end": "14:00" }
  ],
  "isActive": true,
  "timezone": "Africa/Nairobi"
}
```

To **block a day** (doctor unavailable):
```json
{ "isActive": false }
```

To **re-enable a blocked day**:
```json
{ "isActive": true }
```

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Schedule updated successfully",
  "data": {
    "id": "6a008588ecae04bb606fd0ca",
    "doctorId": "69f8846c319d59e154fdab3c",
    "dayOfWeek": 0,
    "windowStart": "08:00",
    "windowEnd": "16:00",
    "slotDuration": 25,
    "breakBlocks": [
      { "start": "13:00", "end": "14:00" }
    ],
    "isActive": true,
    "timezone": "Africa/Nairobi",
    "createdAt": "2026-05-10T13:18:00.242234Z",
    "updatedAt": "2026-05-11T08:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"day query parameter (0-6) is required"` |
| 400 | `"slotDuration must be 25 or 30"` |
| 400 | `"windowStart must be in HH:mm format"` |
| 400 | `"at least one field must be provided"` |
| 404 | `"Schedule not found for day 0"` |

---

## A4. Deactivate a Day

Blocks an entire day — sets `isActive: false`. No appointments can be booked on this day until re-enabled via A3.

**`DELETE /scheduling/availability?day=0`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `day` | Int (0–6) | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Schedule deactivated successfully",
  "data": {
    "id": "6a008588ecae04bb606fd0ca",
    "doctorId": "69f8846c319d59e154fdab3c",
    "dayOfWeek": 0,
    "windowStart": "09:00",
    "windowEnd": "17:00",
    "slotDuration": 30,
    "breakBlocks": [],
    "isActive": false,
    "timezone": "Africa/Nairobi",
    "createdAt": "2026-05-10T13:18:00.242234Z",
    "updatedAt": "2026-05-11T08:00:00.000000Z"
  }
}
```

---

---

# SECTION B — Availability (PATIENT)

---

## B1. Get Available Slots for a Date

Returns a flat list of available `"HH:mm"` slot start times for a specific doctor on a specific date. Use this to populate a time picker after the patient selects a date.

**`GET /scheduling/availability?doctorId=<id>&date=2026-05-15`**  
**Role:** `PATIENT`

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `doctorId` | String | Yes | The doctor's user ID |
| `date` | String | Yes | `YYYY-MM-DD` format |

### Response `200` — slots available

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Available slots retrieved",
  "data": [
    "09:00",
    "09:30",
    "10:00",
    "10:30",
    "11:00",
    "11:30",
    "14:00",
    "14:30",
    "15:00",
    "15:30",
    "16:00",
    "16:30"
  ]
}
```

### Response `200` — no slots (doctor unavailable or day fully booked)

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Doctor not available on this day",
  "data": []
}
```

### What causes an empty list

| Cause | `message` |
|-------|-----------|
| No schedule configured for that day | `"No availability for this day"` |
| Schedule exists but `isActive: false` | `"Doctor not available on this day"` |
| All slots booked or in the past | `"Available slots retrieved"` with `data: []` |

> When `data` is an empty array, **disable the booking flow** for that date. Show the appropriate message based on the `message` field.

### How slots are computed

1. Generate all slots from `windowStart` to `windowEnd` in `slotDuration` steps
2. Remove any slot overlapping a `breakBlock`
3. Remove slots already BOOKED or CONFIRMED by another patient
4. When querying **today**, remove slots within 5 minutes of current time in the doctor's timezone
5. Return the remaining slots sorted ascending

---

## B2. Calendar View

Returns a day-by-day breakdown for a date range. Use for rendering a calendar UI where the patient browses months or weeks before picking a date.

**`GET /scheduling/calendar?doctorId=<id>&view=month&date=2026-05-01&forDoctor=false`**  
**Role:** `DOCTOR` or `PATIENT`

### Query Parameters

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `doctorId` | String | Yes | — | Doctor's user ID |
| `view` | String | No | `month` | `day` / `week` / `month` |
| `date` | String | No | Today | Any date within the desired range (`YYYY-MM-DD`) |
| `forDoctor` | Boolean | No | `false` | `true` = include all slot statuses + appointment metadata; `false` = AVAILABLE only |

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
        "date": "2026-05-12",
        "dayOfWeek": "MONDAY",
        "isWorkingDay": true,
        "slots": [
          {
            "slotStart": "09:00",
            "slotEnd": "09:30",
            "status": "AVAILABLE",
            "appointmentId": null,
            "patientId": null
          },
          {
            "slotStart": "09:30",
            "slotEnd": "10:00",
            "status": "BOOKED",
            "appointmentId": "6a1234abcd5678ef90123456",
            "patientId": "69abc123def456789012abcd"
          },
          {
            "slotStart": "12:00",
            "slotEnd": "12:30",
            "status": "BLOCKED",
            "appointmentId": null,
            "patientId": null
          }
        ]
      },
      {
        "date": "2026-05-13",
        "dayOfWeek": "TUESDAY",
        "isWorkingDay": false,
        "slots": []
      }
    ]
  }
}
```

### Slot statuses

| Status | Meaning | Visible when `forDoctor=false`? |
|--------|---------|--------------------------------|
| `AVAILABLE` | Can be booked | Yes |
| `BOOKED` | Patient booked, doctor not yet confirmed | No |
| `CONFIRMED` | Doctor confirmed the appointment | No |
| `CANCELLED` | Appointment was cancelled | No |
| `BLOCKED` | Overlaps a break block or manual override | No |

> `appointmentId` and `patientId` are only populated when `forDoctor=true`.

### `isWorkingDay` rules

| `isWorkingDay` | `slots` | Meaning |
|----------------|---------|---------|
| `false` | `[]` | No schedule configured, or `isActive: false` |
| `true` | non-empty | Working day with future slots |
| `true` | `[]` | Working day but all slots have passed (today after closing time) |

**Do not treat `isWorkingDay: true` + `slots: []` as a day off.** If today, show "No remaining slots today".

---

---

# SECTION C — Appointments (PATIENT)

---

## C1. Book an Appointment

**`POST /scheduling/appointments`**  
**Role:** `PATIENT`

### Request

```json
{
  "doctorId": "69f8846c319d59e154fdab3c",
  "date": "2026-05-15",
  "slotStart": "09:30",
  "notes": "Persistent headache for 3 days"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `doctorId` | String | Yes | Must be a valid doctor ID |
| `date` | String | Yes | `YYYY-MM-DD` format |
| `slotStart` | String | Yes | `HH:mm` format. Must be in the available slot list |
| `notes` | String | No | Optional patient notes |

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "BOOKED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
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
| 400 | `"doctorId is required"` | Validate input |
| 400 | `"date is required"` | Validate input |
| 400 | `"date must be a valid ISO date (YYYY-MM-DD)"` | Validate date format |
| 400 | `"slotStart is required"` | Validate input |
| 400 | `"slotStart must be in HH:mm format"` | Validate time format |
| 400 | `"Invalid date format, expected YYYY-MM-DD"` | Fix date format |
| 400 | `"Doctor has no schedule for this day"` | Refresh availability — day removed |
| 400 | `"Doctor is not available on this day"` | Refresh availability — day deactivated |
| 400 | `"Slot 09:30 is not available"` | Slot taken or past — refresh and re-pick |
| 409 | `"This slot has already been booked"` | Race condition — refresh and re-pick |

> **Always fetch fresh slots (B1) before showing the booking confirmation screen.** If you get a 400 or 409, call B1 again and show the updated slot list.

---

## C2. Get My Appointments (Patient)

Returns all appointments for the authenticated patient.

**`GET /scheduling/appointments/patient`**  
**Role:** `PATIENT`

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
      "date": "2026-05-15",
      "slotStart": "09:30",
      "slotEnd": "10:00",
      "status": "CONFIRMED",
      "bookedAt": "2026-05-11T08:30:00.123456Z",
      "notes": "Persistent headache for 3 days",
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": "2026-05-11T09:00:00.000000Z"
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

---

## C3. Get Single Appointment

**`GET /scheduling/appointments?id=<appointmentId>`**  
**Role:** `DOCTOR` or `PATIENT`

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "BOOKED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": null
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Appointment not found"` |

---

## C4. Cancel an Appointment (Patient)

A patient can cancel their own appointment at any time before it is COMPLETED.

**`PATCH /scheduling/appointments/cancel?id=<appointmentId>`**  
**Role:** `PATIENT`

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "CANCELLED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": "I am feeling better",
    "cancelledBy": "69abc123def456789012abcd",
    "updatedAt": "2026-05-12T07:00:00.000000Z"
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

# SECTION D — Appointments (DOCTOR)

---

## D1. Get My Appointments (Doctor)

Returns enriched appointments for the authenticated doctor. `patientName` and `doctorSpecialities` are resolved server-side — no extra lookups needed on the client.

**`GET /scheduling/appointments/doctor/all`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `filter` | String | No | `upcoming` / `past` / `today` — omit for all |

| Filter value | Returns |
|-------------|---------|
| `upcoming` | date ≥ today AND status is `BOOKED` or `CONFIRMED` |
| `past` | date < today (all statuses) |
| `today` | date == today (all statuses) |
| *(none)* | All appointments |

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
      "patientName": "Jane Doe",
      "doctorSpecialities": ["Cardiology", "Internal Medicine"],
      "date": "2026-05-15",
      "slotStart": "09:30",
      "slotEnd": "10:00",
      "status": "BOOKED",
      "bookedAt": "2026-05-11T08:30:00.123456Z",
      "notes": "Persistent headache for 3 days",
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": null
    }
  ]
}
```

---

## D2. Get Appointments for a Specific Date

Returns a simple list (no enrichment) for a single date. Useful for a daily agenda view.

**`GET /scheduling/appointments/doctor?date=2026-05-15`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `date` | String (`YYYY-MM-DD`) | Yes |

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
      "date": "2026-05-15",
      "slotStart": "09:30",
      "slotEnd": "10:00",
      "status": "BOOKED",
      "bookedAt": "2026-05-11T08:30:00.123456Z",
      "notes": "Persistent headache for 3 days",
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": null
    }
  ]
}
```

---

## D3. Confirm an Appointment

Doctor acknowledges a patient's booking.

**`PATCH /scheduling/appointments/confirm?id=<appointmentId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body required.

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "CONFIRMED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": "2026-05-11T09:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Appointment not found"` |
| 403 | `"Not authorized to confirm this appointment"` |

---

## D4. Complete an Appointment

Mark an appointment as done after the visit.

**`PATCH /scheduling/appointments/complete?id=<appointmentId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body required.

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "COMPLETED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": "2026-05-15T10:05:00.000000Z"
  }
}
```

---

## D5. Mark No-Show

Patient did not attend their appointment.

**`PATCH /scheduling/appointments/no-show?id=<appointmentId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body required.

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "NO_SHOW",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": null,
    "cancellationReason": null,
    "cancelledBy": null,
    "updatedAt": "2026-05-15T10:05:00.000000Z"
  }
}
```

---

## D6. Cancel an Appointment (Doctor)

Doctor cancels an appointment (e.g. emergency, unavailability).

**`PATCH /scheduling/appointments/cancel?id=<appointmentId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Request

```json
{
  "reason": "Doctor unavailable due to emergency"
}
```

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
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "CANCELLED",
    "bookedAt": "2026-05-11T08:30:00.123456Z",
    "notes": "Persistent headache for 3 days",
    "cancellationReason": "Doctor unavailable due to emergency",
    "cancelledBy": "69f8846c319d59e154fdab3c",
    "updatedAt": "2026-05-14T18:00:00.000000Z"
  }
}
```

---

---

# SECTION E — Appointment Status Reference

## Status Lifecycle

```
BOOKED ──────────────────────► CANCELLED  (by patient or doctor)
  │
  └─► CONFIRMED ──────────────► CANCELLED  (by patient or doctor)
        │
        ├─► COMPLETED           (by doctor, after visit)
        └─► NO_SHOW             (by doctor, patient didn't attend)
```

## Status Meanings

| Status | Meaning | Who sets it |
|--------|---------|-------------|
| `BOOKED` | Patient created the booking | System (on book) |
| `CONFIRMED` | Doctor acknowledged | Doctor |
| `COMPLETED` | Visit took place | Doctor |
| `CANCELLED` | Cancelled before completion | Doctor or Patient |
| `NO_SHOW` | Patient didn't attend | Doctor |

## `cancelledBy` field

When status is `CANCELLED`, `cancelledBy` contains the **user ID** of whoever cancelled. Compare it against the `doctorId` or `patientId` to determine who initiated the cancellation.

---

---

# SECTION F — Common Patterns & Notes

## F1. Booking Flow (Patient)

```
1. Patient selects a doctor
2. GET /scheduling/calendar?doctorId=X&view=month&date=YYYY-MM-01&forDoctor=false
   → Highlight days where at least one slot exists (isWorkingDay: true AND slots non-empty)
3. Patient taps a highlighted day
4. GET /scheduling/availability?doctorId=X&date=YYYY-MM-DD
   → Show available time slots as a picker
5. Patient selects a slot and taps "Book"
6. POST /scheduling/appointments
   → On 201: show booking confirmation
   → On 400 "Slot not available" or 409: refresh slots (step 4) and prompt re-select
```

## F2. Doctor Dashboard Flow

```
1. GET /scheduling/appointments/doctor/all?filter=today
   → Show today's patient list on home screen

2. GET /scheduling/appointments/doctor/all?filter=upcoming
   → Show upcoming tab

3. GET /scheduling/appointments/doctor/all?filter=past
   → Show history tab

4. Tap appointment → GET /scheduling/appointments?id=X → show detail

5. Confirm: PATCH /scheduling/appointments/confirm?id=X
6. Complete: PATCH /scheduling/appointments/complete?id=X
7. No-show:  PATCH /scheduling/appointments/no-show?id=X
8. Cancel:   PATCH /scheduling/appointments/cancel?id=X  { "reason": "..." }
```

## F3. Doctor Schedule Setup Flow

```
1. GET /scheduling/availability/schedule
   → Load current config to pre-fill the settings screen

2. For each day the doctor wants to configure:
   POST /scheduling/availability  { dayOfWeek, windowStart, windowEnd, slotDuration, breakBlocks, isActive }

3. To toggle a day off:
   PUT /scheduling/availability?day=X  { "isActive": false }

4. To toggle a day on:
   PUT /scheduling/availability?day=X  { "isActive": true }
```

## F4. Validation Rules Summary

| Field | Rule |
|-------|------|
| `dayOfWeek` | Integer 0–6 only |
| `windowStart` / `windowEnd` | `HH:mm` 24-hour, e.g. `"09:00"`, `"17:30"` |
| `windowEnd` | Must be strictly after `windowStart` |
| `slotDuration` | Only `25` or `30` are accepted |
| `date` | `YYYY-MM-DD`, e.g. `"2026-05-15"` |
| `slotStart` | `HH:mm` 24-hour format |
| `slotStart` at booking | Must be in the available slot list returned by B1 |

## F5. Empty Slots on Today

`isWorkingDay: true` with an empty `slots` array on today's date means the doctor's working window has already passed for the day (e.g. it is 19:00 and the window is 09:00–17:00). **This is not an error.** Display "No remaining slots today" and let the patient pick another date.
