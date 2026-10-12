# Backend (Codex) Complete Master Instructions: Location Resolution, Comments Persistence & Endpoint Reliability

This document provides exact specifications for backend engineers to resolve **Location Defaulting ("Matero Ward 1")**, **Comments Persistence**, and **Complaints Endpoint Reliability**.

---

## 1. Location Resolution & Eliminating "Matero Ward 1" Defaulting

### Problem Summary
When a citizen submits a complaint with custom or short location text (e.g., `location: "Chibolya"`, `"Kalingalinga"`, or `"manzi yapaka pa kalingalinga"`), the backend currently fails to match an exact ward in the `locations` table and **falls back to defaulting `location_id` / `ward` to "Matero Ward 1"**.

On subsequent `GET /complaints` or `GET /complaints/{id}` calls, the backend returns `"location": { "ward": "Matero Ward 1", "displayName": "Matero Ward 1" }`, overriding custom user location strings across the server and client platforms.

### Required Backend Fixes

1. **Store Raw Submitted Location Text**:
   - Ensure the `complaints` database table has a column `location_text` (or `location`).
   - When handling `POST /complaints` (JSON or multipart), ALWAYS write the exact raw string received in `location` into `complaints.location_text`.

2. **Remove Hardcoded Default/Fallback to "Matero Ward 1"**:
   - Do **NOT** default unmatched location strings to Matero Ward 1 or the first database location record.
   - Implement the following location resolution hierarchy:
     1. If `locationId` is provided and exists in `locations` table, set `location_id = locationId`.
     2. Else if `ward` / `district` / `province` is provided, attempt case-insensitive lookup in `locations` table.
     3. Else perform a **bidirectional substring lookup** on `location_text` against known ward names in `locations` table (e.g., `"Chibolya"` in `location_text` -> `Chibolya Ward 14`, or `"Kalingalinga"` -> `Kalingalinga Ward 23`).
     4. **CRITICAL FALLBACK RULE**: If NO matching location is found in the `locations` table, set `location_id = NULL`. **Do NOT assign Matero Ward 1's ID.**

3. **DTO Response Serialization (`GET /complaints` & `GET /complaints/{id}`)**:
   - If `location_id` is set: return the matched location object:
     ```json
     "location": {
       "id": "location-uuid",
       "ward": "Chibolya Ward 14",
       "district": "Lusaka",
       "province": "Lusaka",
       "displayName": "Chibolya Ward 14"
     }
     ```
   - If `location_id` is `NULL` (custom/unmatched text): return the stored `location_text` string as display name:
     ```json
     "location": {
       "id": null,
       "ward": "Chibolya",
       "district": "Lusaka",
       "province": "Lusaka",
       "displayName": "Chibolya"
     }
     ```

---

## 2. Comment Parsing, Storage & Retrieval

### Problem Summary
User comments submitted via `POST /complaints/{id}/comments` are currently failing to persist in the backend database, resulting in empty or missing comment threads when fetched via `GET /complaints/{id}` or `GET /complaints/{id}/comments`.

### Required Backend Fixes

1. **Database Table Schema (`comments`)**:
   Ensure the SQL database contains the `comments` table with the following schema:
   ```sql
   CREATE TABLE IF NOT EXISTS comments (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
       user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
       content TEXT NOT NULL,
       is_official BOOLEAN NOT NULL DEFAULT FALSE,
       is_internal BOOLEAN NOT NULL DEFAULT FALSE,
       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
   );
   
   CREATE INDEX IF NOT EXISTS idx_comments_complaint_id ON comments(complaint_id);
   ```

2. **Implement `POST /complaints/{id}/comments`**:
   - Route: `POST /complaints/:id/comments` (Bearer Authentication Required)
   - Extract `complaint_id` from URL path parameter `:id`.
   - Extract `user_id` from authenticated JWT token (`req.user.id`).
   - **Body Field Handling**: Read `req.body.message || req.body.content` (the mobile app sends `{ "message": "...", "isCitizenVisible": true }`):
     ```js
     const content = req.body.message || req.body.content;
     ```
   - Validate `content` is not null, not empty, and trimmed.
   - Insert record into `comments` table:
     ```sql
     INSERT INTO comments (id, complaint_id, user_id, content, is_official, created_at)
     VALUES (gen_random_uuid(), :complaint_id, :user_id, :content, FALSE, NOW());
     ```
   - Return `HTTP 201 Created`:
     ```json
     {
       "success": true,
       "message": "Comment posted successfully",
       "data": {
         "id": "c1a2b3c4-5678-90ab-cdef-1234567890ab",
         "complaintId": "c25fd89a-7945-428c-a56c-74a8fa452ab4",
         "userId": "user-uuid-here",
         "content": "Water pipe leak has been repaired in Kalingalinga, thank you!",
         "isOfficial": false,
         "authorName": "Jane Mwanza",
         "createdAt": "2026-10-12T20:00:00.000Z"
       }
     }
     ```

3. **Implement `GET /complaints/{id}/comments`**:
   - Route: `GET /complaints/:id/comments`
   - Execute query:
     ```sql
     SELECT 
         c.id, 
         c.complaint_id AS "complaintId", 
         c.user_id AS "userId", 
         c.content, 
         c.is_official AS "isOfficial", 
         c.created_at AS "createdAt",
         CONCAT(u.first_name, ' ', u.last_name) AS "authorName"
     FROM comments c
     JOIN users u ON c.user_id = u.id
     WHERE c.complaint_id = :complaintId
       AND c.is_internal = FALSE
     ORDER BY c.created_at ASC;
     ```
   - Return `HTTP 200 OK`:
     ```json
     [
       {
         "id": "comment-uuid-1",
         "authorName": "Jane Mwanza",
         "content": "I can confirm the water is flowing now.",
         "isOfficial": false,
         "createdAt": "2026-10-12T19:30:00.000Z"
       },
       {
         "id": "comment-uuid-2",
         "authorName": "Lusaka Water Staff",
         "content": "Our repair team has completed the work.",
         "isOfficial": true,
         "createdAt": "2026-10-12T19:45:00.000Z"
       }
     ]
     ```

4. **Include Comments Array in `GET /complaints/{id}` Detail Response**:
   - When returning detail for `GET /complaints/{id}`, include the populated `comments` array directly inside the response JSON payload.

---

## 3. Complaints Endpoint Reliability (HTTP 500 Prevention)

### Problem Summary
Ensure `GET /api/v1/complaints`, `GET /api/v1/complaints/my`, and `POST /api/v1/complaints` execute cleanly without throwing unhandled exceptions.

### Required Backend Fixes
1. **Null-Safety**: Ensure queries handle complaints where `location_id`, `category_id`, or `assigned_to` is `NULL` without throwing Null Pointer exceptions.
2. **Error Isolation**: Wrap handlers in `try-catch` blocks so endpoints return clean JSON error payloads instead of crashing the server instance.

---

## 4. Master Checklist for Backend Engineers

- [ ] Remove hardcoded `"Matero Ward 1"` fallback logic in location resolver. Set `location_id = NULL` for unmatched text and preserve `location_text`.
- [ ] Support bidirectional location substring matching (`"Chibolya"` -> `Chibolya Ward 14`).
- [ ] Read `req.body.message || req.body.content` in `POST /complaints/:id/comments`.
- [ ] Insert comment entries into `comments` SQL table and return HTTP 201 response.
- [ ] Return sorted comments array in `GET /complaints/:id/comments` and `GET /complaints/:id`.
