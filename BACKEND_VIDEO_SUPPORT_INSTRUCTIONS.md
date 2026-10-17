# Backend Implementation Guide: Video Attachments & Processing (Max 90s)

This document specifies the required REST API endpoints, database schemas, and storage specifications to support **video complaint attachments (up to 1 minute 30 seconds / 90s)** in the **Voice Yanga** platform.

---

## 1. Video Attachment Constraints & Limits

- **Maximum Duration**: 90 seconds (1 minute 30 seconds).
- **Allowed MIME Types**: `video/mp4`, `video/webm`, `video/3gpp`, `video/quicktime`.
- **Maximum File Size**: 25 MB.
- **Client Compression**: The Android app compresses recorded videos to 720p H.264/AAC before uploading to optimize bandwidth over mobile data.

---

## 2. API Endpoints

### 2.1 Video Upload Endpoint: `POST /videos`

Allows uploading a standalone video file prior to complaint creation or during multipart submission.

- **Request Headers**:
  - `Authorization`: `Bearer <jwt_token>`
  - `Content-Type`: `multipart/form-data`

- **Form-Data Parts**:
  - `file`: Video binary stream (`video/mp4`)
  - `durationSeconds`: Integer string (e.g., `"45"`)

- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Video uploaded successfully",
    "data": {
      "id": "media_vid_123",
      "fileUrl": "https://storage.voiceyanga.com/videos/v_12345678.mp4",
      "thumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_12345678.jpg",
      "durationSeconds": 45,
      "mimeType": "video/mp4"
    }
  }
  ```

---

### 2.2 Complaint Creation / Retrieval Schema Extensions

#### `POST /complaints` (JSON Payload)
When submitting a complaint with an uploaded video, include `videoUrl` and `videoDurationSeconds`:

```json
{
  "title": "Flooded Crossing Bridge",
  "description": "Video evidence showing deep water overflow on crossing bridge.",
  "category": "Roads & Transport",
  "location": "Chibolya Ward 14",
  "priority": "HIGH",
  "clientUuid": "uuid_789",
  "videoUrl": "https://storage.voiceyanga.com/videos/v_12345678.mp4",
  "videoDurationSeconds": 45
}
```

#### `GET /complaints` & `GET /complaints/{id}` Responses
Ensure complaint DTOs and `media` arrays include video entries:

```json
{
  "id": "complaint_99",
  "title": "Flooded Crossing Bridge",
  "videoUrl": "https://storage.voiceyanga.com/videos/v_12345678.mp4",
  "videoThumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_12345678.jpg",
  "videoDurationSeconds": 45,
  "media": [
    {
      "id": "media_vid_123",
      "type": "VIDEO",
      "fileUrl": "https://storage.voiceyanga.com/videos/v_12345678.mp4",
      "thumbnailUrl": "https://storage.voiceyanga.com/thumbnails/v_12345678.jpg"
    }
  ]
}
```

---

## 3. Storage & Storage Security Guidelines

1. **Auto Thumbnail Generation**: Upon receiving a video upload, the backend should ideally trigger an asynchronous worker (e.g., FFmpeg) to extract a JPEG thumbnail at 1.0 second timestamp.
2. **CDN Delivery**: Videos should be served over HTTP Range Request enabled CDNs so mobile video players (Media3 ExoPlayer) can buffer and stream video segments seamlessly.
3. **Storage Cleanup**: Cascading delete on `DELETE /complaints/{id}` must purge stored video files and generated thumbnails.
