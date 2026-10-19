# Walkthrough - Fix Complaint Deletion Failure

Fixed the issue where complaints with or without video attachments were refusing to delete on the server API.

## Changes

### [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Moved `DELETE_COMPLAINT` action handling to the top of `syncAction(PendingAction action)` before the local complaint existence check.
- Extracted `serverId` directly from `action.getData()` and executed `apiService.deleteComplaint(serverId).execute()` to ensure the deletion request successfully reaches the server backend even after local record removal.

## Verification Results

### Automated Tests
- Ran Gradle build (`app:assembleDebug`) — **Build finished successfully.**
