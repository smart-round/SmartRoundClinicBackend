# Scheduling Module — Frontend Integration Guide

## Overview

The scheduling module handles three concerns:

1. **Doctor availability** — the doctor defines which days and hours they work each week
2. **Slot availability** — the patient queries which specific time slots are open on a given date
3. **Appointments** — the patient books a slot; both parties can then manage its lifecycle

All endpoints require a valid JWT (`Authorization: Bearer <accessToken>`).

---

## Day-of-Week Mapping

Slots and schedules use ISO-8601 ordinal values (Monday-first):

| Value | Day |
|-------|-----|
| 0 | Monday |
| 1 | Tuesday |
| 2 | Wednesday |
| 3 | Thursday |
| 4 | Friday |
| 5 | Saturday |
| 6 | Sunday |

---

## Part 1 — Doctor: Managing Weekly Schedule

### 1.1 Create / Update a Day's Schedule

A doctor defines one schedule entry per day of the week. Calling this endpoint again for the same `dayOfWeek` updates the existing entry (upsert).

**`POST /scheduling/availability`** — Role: `DOCTOR`

Request body:
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

| Field | Type | Description |
|-------|------|-------------|
| `dayOfWeek` | Int (0–6) | Day to configure (0 = Monday) |
| `windowStart` | `"HH:mm"` | When the doctor starts accepting patients |
| `windowEnd` | `"HH:mm"` | When the doctor stops accepting patients |
| `slotDuration` | Int (minutes) | Length of each appointment slot |
| `breakBlocks` | Array | Time ranges when no slots are available (e.g. lunch) |
| `isActive` | Boolean | `false` blocks the entire day — no slots will be bookable |
| `timezone` | String | IANA timezone, default `"Africa/Nairobi"` |

**To mark a day as unavailable**, set `isActive: false`. Alternatively, just don't create a schedule entry for that day — the engine treats missing days as unavailable.

Repeat for each working day. A typical doctor setup is 7 POST calls (Mon–Sun).

---

### 1.2 View Own Weekly Schedule

**`GET /scheduling/availability/schedule`** — Role: `DOCTOR`

No parameters. Returns all configured days for the authenticated doctor.

```json
{
  "data": [
    {
      "id": "...",
      "doctorId": "...",
      "dayOfWeek": 0,
      "windowStart": "09:00",
      "windowEnd": "17:00",
      "slotDuration": 30,
      "breakBlocks": [{ "start": "12:00", "end": "14:00" }],
      "isActive": true,
      "timezone": "Africa/Nairobi",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

Use this to populate a schedule settings page so the doctor can see their current configuration before editing.

---

### 1.3 Update a Single Day

**`PUT /scheduling/availability?day=0`** — Role: `DOCTOR`

All fields are optional — only send what needs to change.

```json
{
  "windowStart": "08:00",
  "isActive": false
}
```

To **block a day** (e.g. doctor is on leave): `{ "isActive": false }`  
To **re-enable a day**: `{ "isActive": true }`

---

### 1.4 Deactivate a Day

**`DELETE /scheduling/availability?day=0`** — Role: `DOCTOR`

Sets `isActive: false` for that day. Equivalent to `PUT` with `{ "isActive": false }`.

---

## Part 2 — Patient: Checking Availability

### 2.1 Query Available Slots for a Date

**`GET /scheduling/availability?doctorId=<id>&date=2026-05-15`** — Role: `PATIENT`

Returns a flat list of available slot start times for a specific doctor on a specific date.

```json
{
  "data": ["09:00", "09:30", "10:00", "10:30", "11:00", "14:00", "14:30"]
}
```

**What the engine filters out:**
- The entire day if `isActive: false` (returns `[]`)
- Slots that overlap a `breakBlock`
- Slots already booked or confirmed by another patient
- Slots in the past (when querying today, slots within 5 minutes of current time are excluded)
- Days with no schedule configured (returns `[]`)

**Frontend flow:**

```
1. Patient picks a doctor
2. Patient picks a date from a calendar
3. Frontend calls GET /scheduling/availability?doctorId=X&date=YYYY-MM-DD
4. Display the returned time slots as selectable buttons
5. Patient picks a slot → proceed to booking
```

> If `data` is an empty array, show "No availability for this date" — do not let the patient proceed to booking.

---

### 2.2 Calendar View (Multi-day)

**`GET /scheduling/calendar?doctorId=<id>&view=month&date=2026-05-01&forDoctor=false`**  
Role: `DOCTOR` or `PATIENT`

Returns a structured day-by-day breakdown for a date range.

| Query param | Values | Description |
|-------------|--------|-------------|
| `doctorId` | String | Required |
| `view` | `day` / `week` / `month` | Default: `month` |
| `date` | `YYYY-MM-DD` | Any date within the desired range |
| `forDoctor` | `true` / `false` | `true` includes all slot statuses + appointmentId/patientId; `false` returns only AVAILABLE slots |

Response shape:
```json
{
  "data": {
    "doctorId": "...",
    "days": [
      {
        "date": "2026-05-12",
        "dayOfWeek": "MONDAY",
        "isWorkingDay": true,
        "slots": [
          {
            "slotStart": "09:00",
            "slotEnd": "09:30",
            "status": "AVAILABLE"
          },
          {
            "slotStart": "09:30",
            "slotEnd": "10:00",
            "status": "BOOKED",
            "appointmentId": "...",
            "patientId": "..."
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

**Slot statuses (when `forDoctor=true`):**

| Status | Meaning |
|--------|---------|
| `AVAILABLE` | Can be booked |
| `BOOKED` | Appointment created, not yet confirmed |
| `CONFIRMED` | Doctor confirmed the appointment |
| `CANCELLED` | Appointment was cancelled |
| `BLOCKED` | Overlaps a break block or a manual override |

**Frontend usage:**

- **Patient calendar**: call with `forDoctor=false`. Only AVAILABLE slots are returned. Highlight working days that have at least one slot. When the patient taps a day, fetch the slot list to show a picker.
- **Doctor dashboard calendar**: call with `forDoctor=true`. Colour-code slots by status. Show appointment details on tap using the `appointmentId`.

> `isWorkingDay: true` with `slots: []` on today means all slots have already passed — display "No remaining slots today" rather than treating it as a non-working day.

---

## Part 3 — Patient: Booking an Appointment

### 3.1 Book

**`POST /scheduling/appointments`** — Role: `PATIENT`

```json
{
  "doctorId": "69f8846c319d59e154fdab3c",
  "date": "2026-05-15",
  "slotStart": "09:30",
  "notes": "Persistent headache for 3 days"
}
```

The backend validates:
1. Date format is `YYYY-MM-DD`
2. Doctor has a schedule for that day and `isActive: true`
3. The requested slot is actually in the computed available list (not in a break, not past, not already taken)
4. No concurrent booking of the same slot (MongoDB transaction)

**Success (201):**
```json
{
  "data": {
    "id": "...",
    "doctorId": "...",
    "patientId": "...",
    "date": "2026-05-15",
    "slotStart": "09:30",
    "slotEnd": "10:00",
    "status": "BOOKED",
    "bookedAt": "...",
    "notes": "Persistent headache for 3 days"
  }
}
```

**Error cases:**

| HTTP | Message | Frontend action |
|------|---------|-----------------|
| 400 | `"Invalid date format, expected YYYY-MM-DD"` | Validate date input client-side |
| 400 | `"Doctor has no schedule for this day"` | Refresh availability — day may have been removed |
| 400 | `"Doctor is not available on this day"` | Refresh availability — day was deactivated |
| 400 | `"Slot 09:30 is not available"` | Slot was taken or is in the past — refresh slots and show picker again |
| 409 | `"This slot has already been booked"` | Race condition — another patient just took it; refresh and re-pick |

> Always re-fetch the slot list immediately before showing the booking confirmation screen to minimise 409 conflicts.

---

## Part 4 — Doctor: Managing Appointments

### 4.1 Get Appointments (Filtered)

**`GET /scheduling/appointments/doctor/all`** — Role: `DOCTOR`

| Query param | Values | Description |
|-------------|--------|-------------|
| `filter` | `upcoming` / `past` / `today` / *(none)* | Omit for all appointments |

| Filter | Returns |
|--------|---------|
| `upcoming` | date ≥ today AND status is BOOKED or CONFIRMED |
| `past` | date < today (all statuses) |
| `today` | date == today (all statuses) |
| *(none)* | Everything |

Response includes enriched data: `patientName` and `doctorSpecialities` are resolved server-side.

```json
{
  "data": [
    {
      "id": "...",
      "doctorId": "...",
      "patientId": "...",
      "patientName": "Jane Doe",
      "doctorSpecialities": ["Cardiology"],
      "date": "2026-05-15",
      "slotStart": "09:30",
      "slotEnd": "10:00",
      "status": "BOOKED",
      "bookedAt": "...",
      "notes": "Persistent headache",
      "cancellationReason": null,
      "cancelledBy": null,
      "updatedAt": null
    }
  ]
}
```

**Suggested frontend tabs:**

```
[ Upcoming ]  [ Today ]  [ Past ]  [ All ]
```

Each tab hits the same endpoint with the corresponding `?filter=` value.

---

### 4.2 Appointment Lifecycle Actions

All PATCH endpoints use the appointment `id` as a **query parameter**: `?id=<appointmentId>`

| Action | Endpoint | Role | When to use |
|--------|----------|------|-------------|
| Confirm | `PATCH /scheduling/appointments/confirm?id=X` | DOCTOR | Doctor acknowledges the booking |
| Complete | `PATCH /scheduling/appointments/complete?id=X` | DOCTOR | After the appointment has taken place |
| No-show | `PATCH /scheduling/appointments/no-show?id=X` | DOCTOR | Patient didn't show up |
| Cancel | `PATCH /scheduling/appointments/cancel?id=X` | DOCTOR or PATIENT | Either party cancels |

**Cancel request body** (reason is optional):
```json
{ "reason": "Doctor unavailable due to emergency" }
```

**Status flow:**

```
BOOKED
  └─► CONFIRMED  (doctor confirms)
        └─► COMPLETED  (after visit)
        └─► NO_SHOW    (patient didn't attend)
  └─► CANCELLED  (either party, any time before COMPLETED)
```

---

### 4.3 Get Single Appointment

**`GET /scheduling/appointments?id=<appointmentId>`** — Role: `DOCTOR` or `PATIENT`

---

### 4.4 Get Appointments for a Specific Date

**`GET /scheduling/appointments/doctor?date=2026-05-15`** — Role: `DOCTOR`

Returns simple appointment list (no enrichment) for one date. Useful for a day-view sidebar.

---

## Part 5 — Patient: Viewing Own Appointments

**`GET /scheduling/appointments/patient`** — Role: `PATIENT`

Returns all appointments for the authenticated patient (no filter params currently). Include the `status` field to let the patient know if their appointment is pending confirmation, confirmed, etc.

---

## Key Rules for the Frontend

1. **Always fetch fresh slots before booking.** The slot list can change between the patient viewing it and confirming. A 409 means someone else just took it — prompt the patient to re-select.

2. **`isWorkingDay: true` + `slots: []` ≠ day off.** It means the doctor is configured for that day but all slots have passed (today after working hours). Show "No remaining slots" not "Unavailable".

3. **`isWorkingDay: false` = no schedule or `isActive: false`.** Don't show a slot picker for these days.

4. **The doctor's `timezone` drives all past-slot filtering.** The server handles this — the frontend just renders what comes back.

5. **`forDoctor=false` on the calendar** strips all non-AVAILABLE slots. Safe to pass to a patient's view without leaking other patients' booking data.

6. **Slot times are always `"HH:mm"` 24-hour format** in the doctor's configured timezone. Display conversion is the frontend's responsibility if the patient is in a different timezone.
