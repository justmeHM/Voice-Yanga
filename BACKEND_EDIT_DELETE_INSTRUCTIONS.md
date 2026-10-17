# Backend Implementation Guide: Edit & Delete for Complaints and Comments

This document specifies the required REST API endpoints, authorization checks, payload formats, and database behavior to support editing and deleting comments and complaints in the **Voice Yanga** platform.

---

## 1. Overview & Security Requirements

### Authentication & Authorization
- **Bearer Token**: All endpoints require an `Authorization: Bearer <JWT_TOKEN>` header.
- **Ownership Verification**:
  - A user can **only** edit or delete comments that they authored.
  - A citizen can **only** edit or delete complaints that they created (`userId` or `authorEmail` matches JWT identity).
- **HTTP Status Codes**:
  - `200 OK`: Operation succeeded.
  - `400 BAD_REQUEST`: Validation failure (empty text, invalid fields).
  - `401 UNAUTHORIZED`: Missing or invalid JWT token.
  - `403 FORBIDDEN`: User does not own the target complaint/comment.
  - `404 NOT_FOUND`: Target complaint or comment ID does not exist.

---

## 2. Comment Endpoints

### 2.1 Edit Comment: `PATCH /complaints/{id}/comments/{commentId}`

Updates the message content of an existing comment.

- **Request Headers**:
  - `Authorization`: `Bearer <jwt_token>`
  - `Content-Type`: `application/json`

- **Request Body**:
  ```json
  {
    "message": "Updated comment content text.",
    "isCitizenVisible": true
  }
  ```

- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Comment updated successfully",
    "data": {
      "id": "comment_uuid_123",
      "authorName": "John Doe",
      "content": "Updated comment content text.",
      "isOfficial": false,
      "createdAt": "2026-03-30T10:15:30.000Z"
    }
  }
  ```

---

### 2.2 Delete Comment: `DELETE /complaints/{id}/comments/{commentId}`

Deletes an existing comment and updates comment counts.

- **Request Headers**:
  - `Authorization`: `Bearer <jwt_token>`

- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Comment deleted successfully"
  }
  ```

- **Database Action**:
  - Delete record from `comments` table where `id = commentId` AND `complaint_id = id`.
  - Recalculate or decrement `commentCount` on `complaints` record.

---

## 3. Complaint Endpoints

### 3.1 Edit Complaint: `PATCH /complaints/{id}`

Updates editable fields (`title`, `description`, `category`, `location`) of a citizen's own report.

- **Request Headers**:
  - `Authorization`: `Bearer <jwt_token>`
  - `Content-Type`: `application/json`

- **Request Body**:
  ```json
  {
    "title": "Updated Report Title",
    "description": "Updated detailed description of the issue.",
    "category": "Roads & Transport",
    "location": "Chibolya Ward 14"
  }
  ```

- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Complaint updated successfully",
    "data": {
      "id": "complaint_uuid_456",
      "referenceCode": "VY-2026-0042",
      "title": "Updated Report Title",
      "description": "Updated detailed description of the issue.",
      "category": {
        "id": "cat_1",
        "name": "Roads & Transport"
      },
      "location": {
        "id": "loc_1",
        "displayName": "Chibolya Ward 14"
      },
      "status": "SUBMITTED",
      "supportCount": 12,
      "updatedAt": "2026-03-30T10:20:00.000Z"
    }
  }
  ```

---

### 3.2 Delete Complaint: `DELETE /complaints/{id}`

Permanently deletes a complaint and all associated child entities.

- **Request Headers**:
  - `Authorization`: `Bearer <jwt_token>`

- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Complaint deleted successfully"
  }
  ```

- **Database Action (Cascading Delete)**:
  - Delete child records from `comments` where `complaint_id = id`.
  - Delete child records from `complaint_media` and purge stored assets (S3/Cloud Storage).
  - Delete records from `supports` / `votes` where `complaint_id = id`.
  - Delete notification entries for this complaint.
  - Delete main record from `complaints` where `id = id`.

---

## 4. Mobile Integration Notes

- **Optimistic UI Updates**: The mobile application updates Room DB immediately when offline or online, then syncs pending actions (`EDIT_COMMENT`, `DELETE_COMMENT`, `EDIT_COMPLAINT`, `DELETE_COMPLAINT`) in the background.
- **Handling 404 Response**: If the server returns `404 NOT_FOUND` during background sync (e.g. comment/complaint already deleted on server), the app considers the sync action fulfilled and purges the local pending action.
