# Backend Codex Master Instructions: Video Support, Comment Deletion Fix & Report Management

This document serves as the **authoritative prompt and technical specification** for Codex / AI agents working on the **Voice Yanga Backend API** (`/api/v1/`).

Following these instructions will ensure that the backend API and Android mobile app work seamlessly as one without breaking existing data or flows.

---

## 1. CRITICAL BUG FIX: Reappearing Deleted Comments

### Root Cause
Currently, when a user deletes a comment, the mobile app sends a deletion request. However, subsequent `GET /complaints/{id}/comments` calls return the comment again because the backend soft-deletes records (`deleted_at = NOW()`) but **fails to filter out soft-deleted comments** in SQL queries.

### Required Backend Changes

#### A. Database Query Filter for `GET /complaints/{id}/comments`
Ensure **ALL** comment retrieval queries filter for non-deleted comments only:

- **SQL Query**:
  ```sql
  SELECT id, complaint_id, user_id, author_name, content, is_official, created_at
  FROM comments
  WHERE complaint_id = $1 AND deleted_at IS NULL
  ORDER BY created_at ASC;
  ```

- **ORM Filter (Prisma / TypeORM / Knex)**:
  ```typescript
  // Prisma
  const comments = await prisma.comment.findMany({
    where: {
      complaintId: id,
      deletedAt: null, // CRITICAL: Exclude soft-deleted comments!
    },
    orderBy: { createdAt: 'asc' },
  });
  ```

#### B. Comment Deletion Endpoints
Support both route formats for comment deletion:
- `DELETE /complaints/{id}/comments/{commentId}`
- `DELETE /comments/{commentId}`

**Behavior**:
1. Verify JWT authorization (`req.user.id` or `req.user.email`).
2. Verify comment ownership (`comment.userId === req.user.id` OR `comment.authorEmail === req.user.email`).
3. Set `deleted_at = NOW()` (or hard delete from `comments` table).
4. Recalculate `commentCount` on `complaints` record:
   ```sql
   UPDATE complaints 
   SET comment_count = (SELECT COUNT(*) FROM comments WHERE complaint_id = $1 AND deleted_at IS NULL)
   WHERE id = $1;
   ```
5. Return `200 OK`:
   ```json
   {
     "success": true,
     "message": "Comment deleted successfully"
   }
   ```

---

## 2. CRITICAL FEATURE: Comment Editing

Support both route formats for comment updates:
- `PATCH /complaints/{id}/comments/{commentId}`
- `PATCH /comments/{commentId}`

### Request
- **Headers**: `Authorization: Bearer <jwt_token>`, `Content-Type: application/json`
- **Body**:
  ```json
  {
    "message": "Updated comment content text.",
    "isCitizenVisible": true
  }
  ```

### Behavior & Verification
1. Verify JWT user ownership (`comment.userId === req.user.id` OR `comment.authorEmail === req.user.email`). Return `403 FORBIDDEN` if not owned by user.
2. Update `content = message` and `updated_at = NOW()`.
3. Return `200 OK`:
  ```json
  {
    "success": true,
    "message": "Comment updated successfully",
    "data": {
      "id": "comment_123",
      "authorName": "John Doe",
      "content": "Updated comment content text.",
      "isOfficial": false,
      "createdAt": "2026-03-30T10:15:30.000Z"
    }
  }
  ```

---

## 3. CRITICAL FEATURE: Video Uploads & Video Metadata (Max 90 Seconds)

### 3.1 Constraints
- **Maximum Duration**: 90 seconds (1 minute 30 seconds). Reject uploads with duration > 90s with `400 BAD_REQUEST`.
- **Maximum File Size**: 25 MB.
- **Allowed MIME Types**: `video/mp4`, `video/webm`, `video/3gpp`, `video/quicktime`.

---

### 3.2 Endpoint: `POST /videos`

- **Headers**: `Authorization: Bearer <jwt_token>`, `Content-Type: multipart/form-data`
- **Form Parts**:
  - `file`: Video binary stream
  - `durationSeconds`: Integer string (e.g. `"45"`)

- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Video uploaded successfully",
    "data": {
      "id": "media_vid_99",
      "fileUrl": "https://storage.voiceyanga.com/videos/v_99.mp4",
      "thumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_99.jpg",
      "durationSeconds": 45
    }
  }
  ```

---

### 3.3 Database Schema Extensions for `complaints` Table

Add columns:
- `video_url` TEXT NULL
- `video_thumbnail_url` TEXT NULL
- `video_duration_seconds` INT NULL DEFAULT 0

---

### 3.4 Complaint DTO & Media List Mapping

In `GET /complaints`, `GET /complaints/my`, and `GET /complaints/{id}`, include video fields in root JSON and in `media` array:

```json
{
  "id": "complaint_456",
  "title": "Severe Bridge Flood",
  "description": "Video evidence showing water overflow.",
  "videoUrl": "https://storage.voiceyanga.com/videos/v_99.mp4",
  "videoThumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_99.jpg",
  "videoDurationSeconds": 45,
  "media": [
    {
      "id": "media_vid_99",
      "type": "VIDEO",
      "fileUrl": "https://storage.voiceyanga.com/videos/v_99.mp4",
      "thumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_99.jpg"
    }
  ]
}
```

---

## 4. CRITICAL FEATURE: Complaint Edit & Delete

### 4.1 Edit Complaint: `PATCH /complaints/{id}`

- **Headers**: `Authorization: Bearer <jwt_token>`, `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "title": "Updated Report Title",
    "description": "Updated detailed description.",
    "category": "Roads & Transport",
    "location": "Chibolya Ward 14"
  }
  ```
- **Validation**: Verify JWT ownership (`complaint.userId === req.user.id` or `complaint.authorEmail === req.user.email`). Return `403 FORBIDDEN` if not owned by user.
- **Response**: `200 OK` with updated complaint payload.

---

### 4.2 Delete Complaint: `DELETE /complaints/{id}`

- **Headers**: `Authorization: Bearer <jwt_token>`
- **Validation**: Verify JWT ownership.
- **Cascading Deletion Rules**:
  1. Delete child records from `comments` where `complaint_id = id`.
  2. Delete child records from `complaint_media` and purge stored assets (S3/Cloud Storage).
  3. Delete records from `supports` / `votes` where `complaint_id = id`.
  4. Delete notifications where `complaint_id = id`.
  5. Delete `complaint_feed_membership` records for `id`.
  6. Delete main record from `complaints` where `id = id`.
- **Response**: `200 OK`:
  ```json
  {
    "success": true,
    "message": "Complaint deleted successfully"
  }
  ```

---

## 5. Firebase Push Notifications (FCM)

When a comment or support is posted to a complaint, send FCM topic notification to `"complaint_" + complaintId`:

```json
{
  "to": "/topics/complaint_456",
  "notification": {
    "title": "New Comment on Report",
    "body": "John Doe commented on 'Severe Bridge Flood'"
  },
  "data": {
    "complaintId": "complaint_456",
    "type": "NEW_COMMENT"
  }
}
```

---

## 6. Summary Checklist for Backend Codex

- [ ] Add `WHERE deleted_at IS NULL` to `GET /complaints/{id}/comments` query.
- [ ] Implement `DELETE /complaints/{id}/comments/{commentId}` & update `commentCount`.
- [ ] Implement `PATCH /complaints/{id}/comments/{commentId}`.
- [ ] Implement `POST /videos` with 90-second duration validation.
- [ ] Add `video_url`, `video_thumbnail_url`, `video_duration_seconds` to `complaints` table & DTOs.
- [ ] Implement `PATCH /complaints/{id}` and `DELETE /complaints/{id}` with cascading deletions.
- [ ] Verify FCM push notifications on topic `"complaint_" + complaintId`.
