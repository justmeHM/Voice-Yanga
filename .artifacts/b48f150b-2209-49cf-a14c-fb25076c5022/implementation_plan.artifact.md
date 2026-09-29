# Implementation Plan: Fix Photos, Comments, and Notifications

Fix specific defects in the Voice Yanga Android app regarding complaint photos, community comments, and notifications as per the provided instructions.

## Proposed Changes

### Core Network Layer

#### [MODIFY] [NetworkModule.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/core/di/NetworkModule.java)
- Update `Authenticator` to handle `401` errors more robustly, even if the "TOKEN_EXPIRED" string is missing from the body (which may happen for image requests).
- Ensure image requests participate in the synchronized refresh flow.
- Ensure the `bearerInterceptor` correctly identifies requests to the API origin (including `/uploads`) to attach the token.

---

### Complaint Comments

#### [MODIFY] [CommentResponse.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/CommentResponse.java)
- Update `@SerializedName` for the message field to `content` to match the server contract.
- Keep the Java field as `content` and update the getter to `getContent()`.
- Ensure `createdAt` is parsed as epoch milliseconds.

#### [MODIFY] [ComplaintRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/ComplaintRepository.java)
- Update `refreshComments` to use the updated `CommentResponse` fields.
- Stop requesting `history` for community complaints if it's not needed for comments, or handle its `403` gracefully without blocking comments.

#### [MODIFY] [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Update mapping to `CommentResponse`.

---

### Notifications

#### [MODIFY] [NotificationDto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/NotificationDto.java)
- Rename `complaintUuid` to `complaintId`.
- Rename `isRead` to `readStatus` (mapping to boolean).
- Rename `timestamp` to `createdAt` (String for ISO parsing).

#### [MODIFY] [NotificationRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/NotificationRepository.java)
- Update mapping logic to use `complaintId` and parse the ISO `createdAt`.
- Ensure `401 -> refresh -> retry` flow is handled correctly for notifications.
- Ensure zero-item response is handled as a valid empty state only after a successful request.

## Verification Plan

### Automated Tests
- Run `NetworkModule` tests to verify token attachment and refresh logic.
- Run `ComplaintRepositoryTest` for comment parsing.
- Run `NotificationRepositoryTest` for notification envelope parsing and mapping.

### Manual Verification
- Deploy to a device/emulator.
- Verify community complaint photos load.
- Verify comments on other citizens' complaints load and display `content` correctly.
- Verify notifications load, show correct counts, and open the corresponding complaint.
