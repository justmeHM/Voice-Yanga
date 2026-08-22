# Iteration 4.5 — Completion of Complaint Creation Gaps

This iteration addresses the missing requirements from Iteration 2 and 3 as identified in the audit, focusing on Location services, Photo attachments, and Requirement Traceability, while maintaining the Java-only and Mock-data constraints.

## Proposed Changes

### Core & Traceability
#### [MODIFY] [MainActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/MainActivity.java)
- Add traceability tags.

#### [MODIFY] [AuthRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/AuthRepository.java)
- Add traceability tags `[FR-AUTH-01]`, `[FR-AUTH-02]`.

### Data Layer
#### [NEW] [ComplaintPhoto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/entity/ComplaintPhoto.java)
- Entity to store local file references for complaint photos `[FR-COMP-03]`.

#### [MODIFY] [ComplaintDao.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/dao/ComplaintDao.java)
- Add methods to insert and retrieve photos.

#### [MODIFY] [AppDatabase.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/database/AppDatabase.java)
- Include `ComplaintPhoto` entity.

#### [MODIFY] [ComplaintRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/ComplaintRepository.java)
- Update `saveComplaint` to handle photo storage.

### UI & Feature Layer
#### [MODIFY] [activity_create_complaint.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/activity_create_complaint.xml)
- Add `RecyclerView` for photo thumbnails.
- Add a `TextView` for displaying and adjusting the detected location.

#### [NEW] [PhotoAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/PhotoAdapter.java)
- Adapter to display selected photos in the creation screen.

#### [MODIFY] [CreateComplaintActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/CreateComplaintActivity.java)
- Implement `FusedLocationProviderClient` for GPS `[FR-COMP-04]`.
- Implement `ActivityResultLauncher` for photo selection (max 5) `[FR-COMP-03]`.
- Wire up the new UI components.

#### [MODIFY] [ComplaintViewModel.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintViewModel.java)
- Add state for selected photo URIs.

### Background Sync
#### [MODIFY] [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Add mock logging for "uploading photos".

## Verification Plan

### Manual Verification
- **Location:** Tap "Use current location", verify the address updates.
- **Photos:** Select up to 5 photos from the gallery, verify they appear in the list and can be removed.
- **Offline Sync:** Create a complaint with photos in airplane mode, turn on internet, and verify `SyncWorker` logs indicate photo upload.
- **Traceability:** Verify `[FR-XXX]` tags are present in the modified source files.
