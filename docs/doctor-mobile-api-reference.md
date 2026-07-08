# Doctor Mobile App — API Integration Reference

This document currently covers **rating a patient** — the doctor-side counterpart to the patient-facing "Rate a Doctor" feature described in `docs/patient-mobile-api-reference.md`. It follows the same conventions as that document so the two can be extended consistently as more doctor-app endpoints are documented.

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

Always check `status` (boolean), not just `httpStatusCode`, when deciding success vs. failure.

**Content-Type:** `application/json`.

---

# SECTION A — Rate a Patient

A doctor can rate a patient once per completed appointment. Ratings feed into that patient's `averageRating` / `totalReviews` fields (returned from `GET /patient/personal-information`).

**Preconditions enforced server-side** (all checked on every submit):
- The appointment referenced by `appointmentId` must exist.
- It must be with the calling doctor (`doctorId` from the JWT, not request body).
- It must be with the specified `patientId`.
- Its `status` must be `COMPLETED`.
- No existing rating for that `appointmentId` (one rating per appointment, not per doctor/patient pair).

---

## A1. Submit a Rating

**`POST /patient/ratings`**
**Auth:** Bearer token required (DOCTOR)

### Request

```json
{
  "appointmentId": "6a5678abcd1234ef90456789",
  "patientId": "69abc123def456789012abcd",
  "rating": 5,
  "comment": "Cooperative, followed pre-visit instructions well."
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `appointmentId` | String | Yes | Must reference a `COMPLETED` appointment with this doctor |
| `patientId` | String | Yes | Must match the appointment's patient |
| `rating` | Int | Yes | `1`–`5` |
| `comment` | String | No | Free text |

`doctorId` is derived from the access token — do not send it.

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "Rating submitted successfully",
  "data": {
    "id": "6c1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 5,
    "comment": "Cooperative, followed pre-visit instructions well.",
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
| 409 | `"This appointment is not with the specified patient"` |
| 409 | `"You can only rate a patient after a completed appointment"` |
| 409 | `"You have already rated this appointment"` |
| 400 | Validation error — `rating` not in `1..5`, or `appointmentId`/`patientId` missing |

---

## A2. Update My Rating

**`PUT /patient/ratings?id=<ratingId>`**
**Auth:** Bearer token required (DOCTOR — must own the rating)

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Request

```json
{
  "rating": 4,
  "comment": "Revising after a second visit."
}
```

Both fields are optional individually, but **at least one is required** — sending both `null` is rejected with `400`. Only the fields you send are changed.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Rating updated successfully",
  "data": {
    "id": "6c1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 4,
    "comment": "Revising after a second visit.",
    "createdAt": "2026-05-20T11:00:00.000000Z",
    "updatedAt": "2026-05-21T09:15:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Rating not found"` — wrong `id`, or the rating belongs to a different doctor |
| 400 | Validation error — `rating` not in `1..5`, or both fields omitted |

---

## A3. Delete My Rating

**`DELETE /patient/ratings?id=<ratingId>`**
**Auth:** Bearer token required (DOCTOR — must own the rating)

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Rating deleted successfully",
  "data": null
}
```

> **Note:** this always returns `200`/`status: true`, even if `id` doesn't match any rating owned by the caller. There is no `404` on delete — if you need to confirm the deletion happened, re-fetch the rating (A4) or the list (A5) afterward.

---

## A4. Get a Single Rating

**`GET /patient/ratings?id=<ratingId>`**
**Auth:** Bearer token required (any role)

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Success",
  "data": {
    "id": "6c1234abcd5678ef90123456",
    "appointmentId": "6a5678abcd1234ef90456789",
    "doctorId": "69f8846c319d59e154fdab3c",
    "patientId": "69abc123def456789012abcd",
    "rating": 4,
    "comment": "Revising after a second visit.",
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

## A5. Get a Patient's Ratings (Paginated)

**`GET /patient/ratings?patientId=<patientId>&page=1&size=20`**
**Auth:** Bearer token required (any role)

### Query Parameters

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `patientId` | String | Yes | |
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
        "id": "6c1234abcd5678ef90123456",
        "appointmentId": "6a5678abcd1234ef90456789",
        "doctorId": "69f8846c319d59e154fdab3c",
        "patientId": "69abc123def456789012abcd",
        "rating": 4,
        "comment": "Revising after a second visit.",
        "createdAt": "2026-05-20T11:00:00.000000Z",
        "updatedAt": "2026-05-21T09:15:00.000000Z"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 20
  }
}
```

> The patient's `averageRating` / `totalReviews` are recomputed automatically after every submit/update/delete and are returned as fields on `GET /patient/personal-information` (see the patient module) — you don't need to compute them client-side.

---

## A6. Complete Flow — Rate a Patient

```
1. Completed appointments list → appointment with status COMPLETED and not yet rated
2. Show star picker + optional comment field
3. POST /patient/ratings  { appointmentId, patientId, rating, comment }
   → 201  → mark this appointment as "rated" locally, show confirmation
   → 409 "You have already rated this appointment"  → treat as already-rated, hide the prompt
   → 409 (other messages)  → surface message, likely a stale local appointment cache — refresh the appointment
4. "My Patient Ratings" screen → GET /patient/ratings?patientId=X or store the returned ratingId per appointment
   → Edit: PUT /patient/ratings?id=X  { rating, comment }
   → Delete: DELETE /patient/ratings?id=X, then re-fetch to confirm
```

---

# SECTION B — Validation Rules Summary

| Field | Rule |
|-------|------|
| `rating` | Integer, `1`–`5` |
| `appointmentId` | Required — must reference a `COMPLETED` appointment with this doctor |
| `patientId` | Required — must match the appointment's patient |
| Update body | At least one of `rating` / `comment` required |

---

# SECTION C — Common Mistakes to Avoid

1. **Treating all `POST /patient/ratings` failures as generic errors.** Every business-rule rejection (appointment not found, not yours, wrong patient, not completed yet, already rated) comes back as HTTP `409`, not `400` or `404`. Branch on `message`, not just the status code, to show the right copy.

2. **Assuming `DELETE /patient/ratings?id=X` failing silently means it worked.** The endpoint returns `200`/`status: true` regardless of whether a matching rating was found. Re-fetch afterward if you need to confirm.

3. **Sending `doctorId` or `patientId` (on update) in the body.** `doctorId` is always taken from the JWT on submit; ownership on update/delete is enforced server-side by matching the token's doctor id against the rating — there's no way to rate or edit on another doctor's behalf.
