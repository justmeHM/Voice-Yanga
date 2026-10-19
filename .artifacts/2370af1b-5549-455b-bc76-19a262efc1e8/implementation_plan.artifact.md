# Implementation Plan - TikTok/YouTube Style Universal Video Media Parsing & Cross-Account Playback Fixes

Comprehensive plan ensuring **every citizen on Voice Yanga** can view and stream uploaded videos across all accounts like Facebook/TikTok/YouTube.

## Comprehensive Root Cause Analysis

1. **Backend Media Type Parsing Bug in `ComplaintRepository`**:
   - **Root Cause**: When the server returns complaint media in `GET /complaints`, `ComplaintRepository` checked `if ("VIDEO".equalsIgnoreCase(m.type))`. If `m.type` was `"video/mp4"`, `"video"`, or `null`, `equalsIgnoreCase("VIDEO")` returned `false`.
   - As a result, the video URL fell through to `m.type == null || "PHOTO".equalsIgnoreCase(...)` and was incorrectly saved as a photo URI, leaving `serverComplaint.getVideoUrl()` as `null` in Room DB for other accounts!
   - **Fix**: Update media type matching in `ComplaintRepository` and `ComplaintDto` so any media item with `mimeType.contains("video")`, `type.contains("video")`, `fileUrl.endsWith(".mp4")`, or `fileUrl.contains("/videos/")` is correctly parsed as a video URL (`setVideoUrl(mediaUrl)`).

2. **ExoPlayer HTTP Data Source Configuration**:
   - Configure ExoPlayer in `ComplaintAdapter` (feed) and `ComplaintDetailActivity` (details) using `DefaultHttpDataSource.Factory` (`allowCrossProtocolRedirects = true`) so ExoPlayer streams public HTTPS video URLs (`https://voice-yanga-api.vercel.app/uploads/videos/...`) smoothly across all user accounts.
   - Use Glide's `asBitmap().frame(1000000)` to extract video poster frames for feed thumbnails like Facebook/TikTok/YouTube.

3. **Category Switch StackOverflow Crash**:
   - **Root Cause**: `selectCategory` called `etTitle.setText(...)` before assigning `selectedCategory`. `etTitle`'s text watcher saw `selectedCategory == null` and called `suggestCategory(...)` $\rightarrow$ `selectCategory(...)` recursively, causing a `StackOverflowError` for non-sensitive categories.
   - **Fix**: Add an `isProgrammaticTitleChange` boolean guard around `etTitle.setText(...)` in `selectCategory(...)`.

4. **Deleted Complaint Returning**:
   - **Root Cause**: `ComplaintRepository.deleteComplaint` deleted the complaint locally first. When `SyncWorker.syncAction` ran, `if (complaint == null) return;` returned early without sending `DELETE /complaints/{id}` to the server. On next feed sync, the server returned the complaint and Room re-inserted it.
   - **Fix**: Move `DELETE_COMPLAINT` handling to the top of `syncAction(...)` in `SyncWorker` before the `complaint == null` check.

## Proposed Changes

### 1. Robust Video Media Parsing
#### [MODIFY] [ComplaintRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/ComplaintRepository.java) & [ComplaintDto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/ComplaintDto.java)
- Update media type classification to detect `VIDEO` by `mimeType`, `type` string, `.mp4` file extension, or `/videos/` path prefix.
- Preserve `existingLocal.getVideoLocalPath()` during server sync upserts.

### 2. Universal ExoPlayer Video Streaming & Feed Thumbnails
#### [MODIFY] [ComplaintDetailActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintDetailActivity.java)
- Use `isMediaAvailable(...)` for local `content://` URI validation.
- Configure ExoPlayer with `DefaultHttpDataSource.Factory` (`allowCrossProtocolRedirects = true`).
- Remove touch-intercepting `setOnClickListener` from `playerViewDetail` and attach fullscreen expansion to `btnFullscreenVideo`.

#### [MODIFY] [ComplaintAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/ComplaintAdapter.java)
- Configure feed ExoPlayer with `DefaultHttpDataSource.Factory`.
- Use `asBitmap().frame(1000000)` on `resolvedMedia` to extract video thumbnail poster frames.

### 3. Form Recursion Guard
#### [MODIFY] [CreateComplaintActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/CreateComplaintActivity.java)
- Add `isProgrammaticTitleChange` guard around `etTitle.setText(...)` in `selectCategory(...)`.

### 4. Server Delete Sync
#### [MODIFY] [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Move `DELETE_COMPLAINT` action processing to top of `syncAction(...)`.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure clean compilation.

### Manual Verification
1. Log in on another account and open a complaint with a video attachment — verify `setVideoUrl` populated Room DB and ExoPlayer streams the remote video smoothly.
2. Select Electricity / Water / Roads in Create Complaint screen — verify no crash occurs.
3. Delete a complaint with a video attachment — verify it is deleted locally and on the server API and does not return on feed refresh.
