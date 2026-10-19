# Authoritative Backend Instructions: Persistent Video Storage & Streaming

This document provides the definitive architectural specification and code instructions for backend engineers maintaining the Voice Yanga REST API to permanently resolve video upload persistence and cross-account playback failures.

---

## 1. Problem Statement: Why Local Disk Storage Fails

### The Ephemeral Serverless Problem (Vercel / AWS Lambda)
Serverless hosting platforms (such as Vercel and AWS Lambda) enforce **ephemeral, read-only file systems**. 
- When mobile clients upload video files via legacy multipart form data written to local disk (e.g., `./uploads/videos/...`), the file is stored temporarily within that specific function invocation container.
- As soon as the container recycles, scales, or handles requests from other serverless instances or different user accounts, **the local file is lost**, resulting in HTTP `404 Not Found` errors when mobile ExoPlayer clients attempt to stream it.
- Furthermore, Vercel enforces a **4.5 MB request payload limit** on serverless function proxies. Direct binary uploads exceeding 4.5 MB fail with HTTP `413 Payload Too Large`.

---

## 2. Solution Architecture: Direct Cloud Storage Uploads

To support video attachments up to **50 MB** and guarantee permanent persistence across all user accounts and sessions, the backend must use **Direct Cloud Storage Uploads** (Vercel Blob, AWS S3, Cloudinary, or Supabase Storage).

### Recommended Workflow
1. **Request Upload URL**: Mobile app calls `POST /api/v1/videos/upload-url` with `{ filename, mimeType, fileSize }`.
2. **Presigned/Blob Token Generation**: Backend returns a secure `uploadUrl` and a permanent public `fileUrl`.
3. **Direct Binary Transfer**: Mobile app uploads the compressed video binary directly to storage (bypassing Vercel serverless proxy).
4. **Complaint Submission**: Mobile app submits the complaint JSON / multipart payload including the permanent `videoUrl`.

---

## 3. Required API Endpoints

### 3.1 Generate Upload URL: `POST /api/v1/videos/upload-url`

- **Headers**:
  ```http
  Authorization: Bearer <jwt_token>
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "filename": "complaint_video_1743436800.mp4",
    "mimeType": "video/mp4",
    "fileSize": 14250120
  }
  ```
- **Response (200 OK / 201 Created)**:
  ```json
  {
    "success": true,
    "message": "Presigned upload URL generated successfully",
    "data": {
      "uploadUrl": "https://blob.vercel-storage.com/videos/v_123456.mp4?token=...",
      "fileUrl": "https://storage.voiceyanga.com/videos/v_123456.mp4",
      "httpMethod": "PUT"
    }
  }
  ```

---

### 3.2 Complaint Creation: `POST /api/v1/complaints`

Include `videoUrl` and `videoDurationSeconds` in the complaint payload:
```json
{
  "title": "Damaged Road Drainage",
  "description": "Overflowing drainage causing major road erosion.",
  "category": "Roads & Transport",
  "location": "Matero, Lusaka",
  "priority": "HIGH",
  "clientUuid": "uuid_789",
  "videoUrl": "https://storage.voiceyanga.com/videos/v_123456.mp4",
  "videoDurationSeconds": 45
}
```

---

## 4. Node.js / Vercel Blob Implementation Example

Install Vercel Blob SDK:
```bash
npm install @vercel/blob
```

### Route Handler: `POST /api/v1/videos/upload-url`
```typescript
import { handleUpload, type HandleUploadBody } from '@vercel/blob/client';
import { NextResponse } from 'next/server';

export async function POST(request: Request): Promise<NextResponse> {
  const body = (await request.json()) as HandleUploadBody;

  try {
    const jsonResponse = await handleUpload({
      body,
      request,
      onBeforeGenerateToken: async (pathname) => {
        return {
          allowedContentTypes: ['video/mp4', 'video/webm', 'video/quicktime', 'video/3gpp'],
          maximumSizeInBytes: 50 * 1024 * 1024, // 50 MB
        };
      },
      onUploadCompleted: async ({ blob }) => {
        console.log('Video uploaded successfully to cloud storage:', blob.url);
      },
    });

    return NextResponse.json(jsonResponse);
  } catch (error) {
    return NextResponse.json(
      { success: false, message: (error as Error).message },
      { status: 400 }
    );
  }
}
```

---

## 5. Storage Headers & Streaming Requirements

To ensure mobile **Media3 ExoPlayer** can buffer and stream video files smoothly across all user accounts:
1. **CORS Headers**: Storage buckets or CDN responses **must** include:
   ```http
   Access-Control-Allow-Origin: *
   Access-Control-Allow-Methods: GET, PUT, OPTIONS
   ```
2. **HTTP Range Requests**: Enable `Accept-Ranges: bytes` support on the storage provider so ExoPlayer can perform chunked streaming without downloading the entire video file upfront.
