# Backend Implementation Instructions: 50 MB Video Uploads via Vercel Blob / S3 Direct Upload

This document serves as the **authoritative prompt and specification for backend engineers / Codex** working on the Voice Yanga Backend REST API (`/api/v1/`).

---

## 1. Problem Statement & Architecture

### Vercel 4.5 MB Limit
Vercel Serverless Functions enforce a **hard platform limit of 4.5 MB** on incoming HTTP request payloads. Directly posting binary video files to `/api/v1/videos` results in HTTP `413 Payload Too Large` from Vercel's edge proxy.

### Solution: Direct Storage Upload (Vercel Blob / AWS S3 / Cloudinary)
To support video uploads up to **50 MB**, the backend must provide a presigned upload URL endpoint (`POST /videos/upload-url`). The mobile app requests an upload URL, posts the video binary directly to Vercel Blob / S3 (bypassing Vercel serverless proxy), and receives the final public video URL.

---

## 2. Endpoint Specification: `POST /videos/upload-url`

### Request Headers
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### Request Body
```json
{
  "filename": "video_1743436800.mp4",
  "mimeType": "video/mp4",
  "fileSize": 18450120
}
```

### Success Response (HTTP 200 OK or 201 Created)
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

## 3. Backend Implementation Example (Node.js & Vercel Blob)

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
        // Authenticate JWT user here
        return {
          allowedContentTypes: ['video/mp4', 'video/webm', 'video/quicktime', 'video/3gpp'],
          tokenPayload: JSON.stringify({ userId: request.user?.id }),
          maximumSizeInBytes: 50 * 1024 * 1024, // 50 MB limit
        };
      },
      onUploadCompleted: async ({ blob, tokenPayload }) => {
        // Optional: Save video metadata into database
        console.log('Video uploaded successfully:', blob.url);
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

## 4. Alternative Implementation (AWS S3 Presigned URL)

If using AWS S3:
```typescript
import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presampler";

const s3 = new S3Client({ region: process.env.AWS_REGION });

app.post('/api/v1/videos/upload-url', async (req, res) => {
  const { filename, mimeType } = req.body;
  const key = `videos/v_${Date.now()}_${filename}`;
  
  const command = new PutObjectCommand({
    Bucket: process.env.S3_BUCKET_NAME,
    Key: key,
    ContentType: mimeType || 'video/mp4',
  });

  const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 3600 });
  const fileUrl = `https://${process.env.S3_BUCKET_NAME}.s3.${process.env.AWS_REGION}.amazonaws.com/${key}`;

  return res.status(200).json({
    success: true,
    data: {
      uploadUrl,
      fileUrl,
      httpMethod: 'PUT'
    }
  });
});
```

---

## 5. Mobile App Fallback Integration

While the backend is deploying `POST /videos/upload-url`, the Android mobile app will:
1. Attempt `POST /videos/upload-url` first.
2. If `POST /videos/upload-url` returns HTTP 404 (endpoint not deployed yet), fall back to high-efficiency client compression (target <= 3.8 MB) on legacy `POST /videos` so no uploads fail during transition.

---

## 6. Checklist for Backend Engineers

- [ ] Add `POST /videos/upload-url` endpoint accepting `{ filename, mimeType, fileSize }`.
- [ ] Set maximum file size limit to **50 MB** (`50 * 1024 * 1024` bytes).
- [ ] Return presigned `uploadUrl`, public `fileUrl`, and `httpMethod: "PUT"`.
- [ ] Verify CORS headers allow `PUT` requests from Android mobile clients.

---

## 7. Video File Streaming & CORS Headers Specification

To ensure videos remain permanently viewable by all citizens across different devices and accounts:
1. **Persistent Cloud Storage**: Verify uploaded video files stored via `POST /videos/upload-url` are persisted in S3 / Vercel Blob / Cloudinary rather than ephemeral `/tmp` storage.
2. **CORS Headers**: Set response header `Access-Control-Allow-Origin: *` on video file URLs so mobile HTTP clients (ExoPlayer / Glide) can stream video files across any domain without cross-origin blocks.
