# Voice Yanga API Alignment Plan

This plan outlines the steps to align the Voice Yanga Android app with the updated API specifications provided in [APP_INTEGRATION_GUIDE.md](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/APP_INTEGRATION_GUIDE.md).

## User Review Required

> [!IMPORTANT]
> The plan involves refactoring the networking layer, including how tokens are refreshed. This will affect how the app handles authentication errors.
>
> [!WARNING]
> I will be moving API configuration from `ApiConstants.java` to `app/build.gradle` using `buildConfigField`. This is a standard Android practice for environment-specific configurations.

## Proposed Changes

### Configuration & Dependencies

#### [MODIFY] [build.gradle](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/build.gradle)
- Update Retrofit and Gson dependencies to version 3.0.0 (as requested in the guide, although 2.x is standard, I'll follow the guide).
- Add `buildConfigField` for `API_BASE_URL` and `API_ORIGIN`.

#### [DELETE] [ApiConstants.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/core/network/ApiConstants.java)
- Remove this file as its content will be moved to `BuildConfig`.

---

### Data Models (DTOs)

#### [NEW] [ApiEnvelope.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/ApiEnvelope.java)
- Create the standard response envelope.

#### [MODIFY] [UserDto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/UserDto.java)
- Align fields with `AppUser`: `id`, `role`, `name`, `firstName`, `lastName`, `email`, `phone`, `permissions`, `createdAt`.

#### [MODIFY] [AuthResponse.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/AuthResponse.java)
- Ensure fields `token`, `refreshToken`, and `user` (AppUser/UserDto) match the guide.

#### [NEW] [RefreshData.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/RefreshData.java), [RefreshRequest.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/RefreshRequest.java)
- Support token refresh.

#### [MODIFY] [ComplaintRequest.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/ComplaintRequest.java)
- Add `priority` field.

#### [NEW] [CreateComplaintResponse.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/CreateComplaintResponse.java)
- Model for complaint creation response.

#### [NEW] [VoiceNoteData.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/VoiceNoteData.java)
- Model for voice note upload response.

#### [NEW] [Pagination models](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/PaginationMeta.java)
- `PaginationMeta`, `PaginatedComplaints`, `PaginatedNotifications`.

#### [NEW] [Other models](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/SupportResponse.java)
- `SupportResponse`, `ComplaintMedia`, `HistoryItem`.

#### [NEW] [ApiError.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/ApiError.java)
- `ApiError` and `FieldError` for better error handling.

---

### Networking Layer

#### [MODIFY] [ApiService.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/api/ApiService.java)
- Refactor to match the guide's `VoiceYangaApi` interface.
- Use `ApiEnvelope<T>` for wrapped responses.
- Add missing endpoints: `getComplaint`, `getHistory`, `getPhotos`, `getMyComplaints`.
- Split `createComplaint` into text-only and multipart.

#### [MODIFY] [NetworkModule.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/core/di/NetworkModule.java)
- Update `OkHttpClient` to include a synchronized refresh logic in the `Authenticator`.
- Update `Retrofit` provider to use `BuildConfig.API_BASE_URL`.

---

### Data & Synchronization

#### [MODIFY] [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Update to use new `ApiService` methods.
- Ensure `clientUuid` is reused correctly.

---

### Logic & Helpers

#### [NEW] [MediaResolver.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/core/utils/MediaResolver.java)
- Implement `resolveMediaUrl` to handle private media access with temporary signed URLs.

## Verification Plan

### Automated Tests
- I will verify the build by running `./gradlew assembleDebug`.
- I will run existing unit tests to ensure no regressions: `./gradlew test`.

### Manual Verification
- Deploy the app to a physical device/emulator.
- Verify registration and login flows.
- Verify complaint submission (text, photos, voice).
- Verify "My Complaints" and detail view.
- Verify notifications.
