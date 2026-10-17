# Implementation Plan - Universal Video Streaming Fix & Incremental Backend CORS/Persistence Note

Ensure **every citizen on Voice Yanga** can view and stream uploaded videos seamlessly across all devices, accounts, and networks.

## Proposed Changes

### 1. Android Client Fixes (Primary Changes)
#### [MODIFY] [ComplaintDetailActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintDetailActivity.java)
- Replace `new File(videoPath).exists()` with `isMediaAvailable(videoPath)` supporting `content://`, `file://`, and HTTP/HTTPS URLs.
- Build ExoPlayer using `DefaultHttpDataSource.Factory` configured with `allowCrossProtocolRedirects = true` and `15s` network timeout so ExoPlayer streams remote video URLs reliably across different accounts.
- Call `detailExoPlayer.setPlayWhenReady(true)` and `detailExoPlayer.play()`.

#### [MODIFY] [ComplaintAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/ComplaintAdapter.java)
- Configure feed ExoPlayer with `DefaultHttpDataSource.Factory` for seamless video streaming across all citizen accounts.
- Use `resolvedMedia` (`https://voice-yanga-api.vercel.app/uploads/videos/...`) for Glide poster frame extraction (`asBitmap().frame(1000000)`).

### 2. Incremental Backend Note (Short & Additive Only)
#### [MODIFY] [BACKEND_VERCEL_50MB_VIDEO_INSTRUCTIONS.md](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/BACKEND_VERCEL_50MB_VIDEO_INSTRUCTIONS.md)
- Add a concise, 1-paragraph note requiring `Access-Control-Allow-Origin: *` headers and public file readability on uploaded video URLs so ExoPlayer can stream video files across any device or domain without re-implementing existing upload endpoints.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure clean compilation.

### Manual Verification
1. Open a complaint with a video attachment from a different account — verify ExoPlayer streams the remote video URL smoothly.
2. Record/attach a new video, open the complaint details — verify the video plays instantly from local `content://` source.
