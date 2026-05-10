# Articles — Doctor Mobile App API Reference

All endpoints require `Authorization: Bearer <accessToken>` in the request header.  
All responses use the standard envelope:

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
  "message": "title is required",
  "data": null
}
```

---

## Article State Lifecycle

```
CREATE
  │
  └─► DRAFT ──────────────────► LIVE       (doctor publishes)
        ▲                          │
        └──────────────────────────┘        (doctor unpublishes → back to DRAFT)
                                   │
                                   └─► DELETED  (doctor deletes — soft delete, permanent)
```

| State | Visible to patients? | Who can set it |
|-------|---------------------|----------------|
| `DRAFT` | No | System (on create), or doctor unpublishing |
| `LIVE` | Yes | Doctor |
| `SUSPENDED` | No | Admin only (moderation) |
| `DELETED` | No | Doctor (permanent — cannot be undone) |

> A `SUSPENDED` article cannot be unpublished or deleted by the doctor. Only admin can act on it.

---

---

# SECTION A — Categories

The doctor must pick a `categoryId` when creating an article. Fetch the category list first to populate the picker.

---

## A1. List All Categories

**`GET /article/categories/all`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Default | Max |
|-------|------|---------|-----|
| `page` | Int | `1` | — |
| `size` | Int | `20` | `100` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Categories retrieved successfully",
  "data": {
    "items": [
      {
        "id": "6a001234abcd5678ef901234",
        "name": "Cardiology",
        "isActive": true,
        "createdAt": "2026-01-15T09:00:00.000000Z",
        "updatedAt": null
      },
      {
        "id": "6a005678abcd1234ef904567",
        "name": "Mental Health",
        "isActive": true,
        "createdAt": "2026-01-16T10:00:00.000000Z",
        "updatedAt": "2026-02-01T08:00:00.000000Z"
      }
    ],
    "total": 12,
    "page": 1,
    "size": 20,
    "pages": 1
  }
}
```

> Only show categories where `isActive: true` in the UI. Inactive categories are retained for historical articles but should not be selectable when creating new ones.

---

---

# SECTION B — Doctor: Managing Own Articles

---

## B1. Create an Article

Articles are created as **multipart/form-data** because they optionally include a thumbnail image upload.

**`POST /article`**  
**Role:** `DOCTOR`  
**Content-Type:** `multipart/form-data`

### Form Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | text | Yes | Article headline |
| `content` | text | Yes | Full article body (plain text or HTML/Markdown — stored as-is) |
| `summary` | text | Yes | Short description shown in article lists |
| `categoryId` | text | Yes | ID from A1 |
| `thumbnail` | file | No | Image file (JPEG / PNG / WebP). Use this **or** `thumbnailUrl`, not both |
| `thumbnailUrl` | text | No | External image URL (e.g. an existing CDN URL). Use this **or** `thumbnail` file, not both |

> New articles are always created in `DRAFT` state. Call B4 to publish.

### Response `201`

```json
{
  "httpStatusCode": 201,
  "status": true,
  "message": "Article created successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension",
    "content": "Hypertension, also known as high blood pressure...",
    "summary": "A comprehensive guide to understanding and managing hypertension.",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
    "state": "DRAFT",
    "datePosted": null,
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": null
  }
}
```

> `thumbnailUrl` in the response is a **presigned URL** (time-limited). Do not store it — re-fetch the article to get a fresh URL when displaying.

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"title is required"` |
| 400 | `"content is required"` |
| 400 | `"summary is required"` |
| 400 | `"categoryId is required"` |
| 404 | `"Article category not found"` |
| 400 | Unsupported image type (non-image file uploaded) |
| 500 | `"Failed to upload thumbnail"` |

---

## B2. Get My Articles

Returns the authenticated doctor's own articles (all states except `DELETED`), paginated.

**`GET /article/my`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Default | Max |
|-------|------|---------|-----|
| `page` | Int | `1` | — |
| `size` | Int | `20` | `100` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Articles retrieved successfully",
  "data": {
    "items": [
      {
        "id": "6a1234abcd5678ef90123456",
        "doctorId": "69f8846c319d59e154fdab3c",
        "title": "Understanding Hypertension",
        "content": "Hypertension, also known as high blood pressure...",
        "summary": "A comprehensive guide to understanding and managing hypertension.",
        "categoryId": "6a001234abcd5678ef901234",
        "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
        "state": "DRAFT",
        "datePosted": null,
        "createdAt": "2026-05-11T08:00:00.000000Z",
        "updatedAt": null
      },
      {
        "id": "6a5678abcd1234ef90456789",
        "doctorId": "69f8846c319d59e154fdab3c",
        "title": "Managing Diabetes Through Diet",
        "content": "Diet plays a crucial role in managing diabetes...",
        "summary": "How dietary changes can significantly improve diabetes management.",
        "categoryId": "6a005678abcd1234ef904567",
        "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a5678abcd1234ef90456789.jpeg",
        "state": "LIVE",
        "datePosted": "2026-04-20T10:00:00.000000Z",
        "createdAt": "2026-04-18T09:00:00.000000Z",
        "updatedAt": "2026-04-20T10:00:00.000000Z"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 20,
    "pages": 1
  }
}
```

**Pagination fields:**

| Field | Description |
|-------|-------------|
| `total` | Total number of articles matching the query |
| `page` | Current page number |
| `size` | Page size requested |
| `pages` | Total number of pages (`ceil(total / size)`) |

---

## B3. Get Single Article

Fetch the full content of any article by ID. Available to any authenticated user.

**`GET /article?id=<articleId>`**  
**Role:** Any authenticated user

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Article retrieved successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension",
    "content": "Hypertension, also known as high blood pressure...",
    "summary": "A comprehensive guide to understanding and managing hypertension.",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
    "state": "LIVE",
    "datePosted": "2026-05-11T09:00:00.000000Z",
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": "2026-05-11T09:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Article not found"` |

---

## B4. Publish an Article

Move an article from `DRAFT` → `LIVE`. Only the doctor who owns the article can publish it.  
`datePosted` is set the **first time** an article goes live and never changes after that.

**`PATCH /article/my/publish?id=<articleId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Article is now live",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension",
    "content": "Hypertension, also known as high blood pressure...",
    "summary": "A comprehensive guide to understanding and managing hypertension.",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
    "state": "LIVE",
    "datePosted": "2026-05-11T09:00:00.000000Z",
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": "2026-05-11T09:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Article not found"` |
| 400 | `"Article is already live"` |
| 400 | `"Cannot publish a deleted article"` |
| 400 | `"You can only publish your own articles"` |

---

## B5. Unpublish an Article (Revert to Draft)

Move an article from `LIVE` → `DRAFT`. The article will no longer be visible to patients.  
`datePosted` is preserved — if the article is published again, it keeps the original `datePosted`.

**`PATCH /article/my/unpublish?id=<articleId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Article moved back to draft",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension",
    "content": "Hypertension, also known as high blood pressure...",
    "summary": "A comprehensive guide to understanding and managing hypertension.",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
    "state": "DRAFT",
    "datePosted": "2026-05-11T09:00:00.000000Z",
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": "2026-05-12T07:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Article not found"` |
| 400 | `"Article is already a draft"` |
| 400 | `"Cannot unpublish a deleted article"` |
| 400 | `"You can only unpublish your own articles"` |

> A `SUSPENDED` article (set by admin) cannot be unpublished by the doctor. Only admin can act on suspended articles.

---

## B6. Update an Article

Updates one or more fields on the doctor's own article. Send only the fields that need to change — all fields are optional.  
The request is **multipart/form-data** to support thumbnail replacement.

**`PUT /article?id=<articleId>`**  
**Role:** `DOCTOR`  
**Content-Type:** `multipart/form-data`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

### Form Fields (all optional)

| Field | Type | Description |
|-------|------|-------------|
| `title` | text | New title |
| `content` | text | New full article body |
| `summary` | text | New short summary |
| `categoryId` | text | Change the category |
| `thumbnail` | file | Replace thumbnail with a new image upload |
| `thumbnailUrl` | text | Replace thumbnail with an external URL |

> If neither `thumbnail` nor `thumbnailUrl` is provided, the existing thumbnail is kept unchanged.  
> If a new `thumbnail` file is provided, the previous image is **replaced** in storage.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Article updated successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension — Updated Guide",
    "content": "Updated content...",
    "summary": "An updated comprehensive guide to understanding and managing hypertension.",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
    "state": "LIVE",
    "datePosted": "2026-05-11T09:00:00.000000Z",
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": "2026-05-13T11:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Article not found"` |
| 400 | `"Cannot update a deleted article"` |
| 400 | `"You can only edit your own articles"` |
| 500 | `"Failed to upload thumbnail"` |

> Updating a `LIVE` article keeps it live. Changes are immediately visible to patients.

---

## B7. Delete an Article

Soft-deletes the article. It disappears from all patient-facing lists immediately.  
**This action is permanent — a deleted article cannot be restored.**

**`DELETE /article?id=<articleId>`**  
**Role:** `DOCTOR`

### Query Parameters

| Param | Type | Required |
|-------|------|----------|
| `id` | String | Yes |

No request body.

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Article deleted successfully",
  "data": {
    "id": "6a1234abcd5678ef90123456",
    "doctorId": "69f8846c319d59e154fdab3c",
    "title": "Understanding Hypertension",
    "content": "...",
    "summary": "...",
    "categoryId": "6a001234abcd5678ef901234",
    "thumbnailUrl": null,
    "state": "DELETED",
    "datePosted": "2026-05-11T09:00:00.000000Z",
    "createdAt": "2026-05-11T08:00:00.000000Z",
    "updatedAt": "2026-05-14T08:00:00.000000Z"
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 404 | `"Article not found"` |
| 400 | `"Article is already deleted"` |
| 400 | `"You can only delete your own articles"` |

---

---

# SECTION C — Browsing Published Articles (Doctor & Patient)

These endpoints are read-only and available to any authenticated user. Doctors can use them to preview their published content as patients would see it.

---

## C1. Get All Live Articles

Returns all `LIVE` articles across all doctors, paginated.

**`GET /article/live`**  
**Role:** Any authenticated user

### Query Parameters

| Param | Type | Default | Max |
|-------|------|---------|-----|
| `page` | Int | `1` | — |
| `size` | Int | `20` | `100` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Articles retrieved successfully",
  "data": {
    "items": [
      {
        "id": "6a1234abcd5678ef90123456",
        "doctorId": "69f8846c319d59e154fdab3c",
        "title": "Understanding Hypertension",
        "content": "Hypertension, also known as high blood pressure...",
        "summary": "A comprehensive guide to understanding and managing hypertension.",
        "categoryId": "6a001234abcd5678ef901234",
        "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
        "state": "LIVE",
        "datePosted": "2026-05-11T09:00:00.000000Z",
        "createdAt": "2026-05-11T08:00:00.000000Z",
        "updatedAt": null
      }
    ],
    "total": 24,
    "page": 1,
    "size": 20,
    "pages": 2
  }
}
```

---

## C2. Get Live Articles by Category

Returns all `LIVE` articles filtered to a specific category, paginated.

**`GET /article/live/category?categoryId=<id>`**  
**Role:** Any authenticated user

### Query Parameters

| Param | Type | Required | Default | Max |
|-------|------|----------|---------|-----|
| `categoryId` | String | Yes | — | — |
| `page` | Int | No | `1` | — |
| `size` | Int | No | `20` | `100` |

### Response `200`

```json
{
  "httpStatusCode": 200,
  "status": true,
  "message": "Articles retrieved successfully",
  "data": {
    "items": [
      {
        "id": "6a1234abcd5678ef90123456",
        "doctorId": "69f8846c319d59e154fdab3c",
        "title": "Understanding Hypertension",
        "content": "Hypertension, also known as high blood pressure...",
        "summary": "A comprehensive guide to understanding and managing hypertension.",
        "categoryId": "6a001234abcd5678ef901234",
        "thumbnailUrl": "https://pub-xxxx.r2.dev/article-thumbnails/6a1234abcd5678ef90123456.jpeg",
        "state": "LIVE",
        "datePosted": "2026-05-11T09:00:00.000000Z",
        "createdAt": "2026-05-11T08:00:00.000000Z",
        "updatedAt": null
      }
    ],
    "total": 6,
    "page": 1,
    "size": 20,
    "pages": 1
  }
}
```

### Error Responses

| HTTP | Message |
|------|---------|
| 400 | `"categoryId query parameter is missing"` |

---

---

# SECTION D — Common Patterns & Notes

## D1. Doctor Article Management Flow

```
1. GET /article/categories/all
   → Load category list to populate the "Category" picker

2. POST /article  (multipart/form-data)
   → Article created as DRAFT
   → Store the returned `id`

3. Edit draft if needed:
   PUT /article?id=X  (multipart/form-data)

4. Preview (optional):
   GET /article?id=X
   → View exactly what the patient will see

5. Publish:
   PATCH /article/my/publish?id=X
   → state becomes LIVE, datePosted is set

6. Edit published article:
   PUT /article?id=X
   → Changes are live immediately

7. Pull article from public view:
   PATCH /article/my/unpublish?id=X
   → state reverts to DRAFT

8. Permanently remove:
   DELETE /article?id=X
   → state becomes DELETED, cannot be undone
```

## D2. My Articles List — Recommended UI States

Display a status badge per article based on `state`:

| State | Badge colour | Label | Actions available |
|-------|-------------|-------|-------------------|
| `DRAFT` | Grey | Draft | Edit, Publish, Delete |
| `LIVE` | Green | Published | Edit, Unpublish, Delete |
| `SUSPENDED` | Orange | Suspended by Admin | None (contact support) |
| `DELETED` | — | — | Should not appear in list |

## D3. Thumbnail Handling

- `thumbnailUrl` in every response is a **presigned URL** — it expires. Do not cache it for more than the session.
- When displaying article cards or detail screens, always use the `thumbnailUrl` from the most recent API response.
- On create or update, you can either:
  - Upload a file directly as the `thumbnail` form field (recommended — stored in R2, CDN-delivered)
  - Pass a public URL as `thumbnailUrl` form field (stored as-is, not uploaded to R2)
- If no thumbnail is provided on create, `thumbnailUrl` in the response will be `null`. Handle this with a fallback image in the UI.

## D4. Content Format

The `content` field is stored and returned exactly as submitted. The app is responsible for rendering it — store it as plain text, HTML, or Markdown depending on your editor choice. The backend does not transform it.

## D5. Pagination

All paginated responses include:

| Field | Description |
|-------|-------------|
| `total` | Total records matching the query |
| `page` | Current page (1-based) |
| `size` | Records per page |
| `pages` | Total pages = `ceil(total / size)` |

Load-more / infinite scroll: increment `page` by 1 until `page >= pages`.
