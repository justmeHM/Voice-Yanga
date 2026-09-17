# Implementation Plan - Alignment with APP_INTEGRATION_GUIDE.md

Strict alignment of the Android app with the `APP_INTEGRATION_GUIDE.md` specifications.

## User Review Required

> [!IMPORTANT]
> - **Registration Logic**: I will change the registration validation to allow *either* Email or Phone, as specified in Section 7 of the guide.
> - **Location Data**: I will add the `constituency` field to `LocationDto` and update the display logic to follow the priority: `ward` > `constituency` > `district`.

## Proposed Changes

### [API & Network]

#### [MODIFY] [ApiService.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/api/ApiService.java)
- Add `@GET("ready")` endpoint as required by Section 1.

#### [MODIFY] [SplashViewModel.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/auth/SplashViewModel.java)
- Update `checkServerHealth` to verify both `/health` and `/ready` endpoints.

### [Data Models & DTOs]

#### [MODIFY] [LocationDto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/LocationDto.java)
- Add `constituency` field.
- Update `getDisplayName()` to incorporate `constituency` in the priority logic required by Section 8.

### [Auth & Registration]

#### [MODIFY] [RegisterActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/auth/RegisterActivity.java)
- Adjust `validate` method to require Email **OR** Phone instead of both.

### [Complaint Submission & Sync]

#### [MODIFY] [ComplaintRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/ComplaintRepository.java)
- Update `scheduleSync` to use unique work names based on `clientUuid` (`complaint-clientUuid`) as per Section 14.
- Update `saveComplaint` to accept the `clientUuid` for the work name.

#### [MODIFY] [SyncWorker.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/SyncWorker.java)
- Ensure the worker correctly handles the updated `LocationDto` display logic if necessary (though DTO change should suffice).

## Verification Plan

### Automated Tests
- Run `SyncWorkerTest` (if exists) to verify unique work scheduling.
- Unit test `LocationDto.getDisplayName()` for correct priority logic.

### Manual Verification
1. **Health Check**: Observe Splash screen behavior with `/health` and `/ready` simulation.
2. **Registration**: Try registering with only Email, then only Phone. Verify it passes validation.
3. **Location**: Verify location selection in "Report Issue" screen displays correctly (Ward, Constituency, District).
4. **Sync**: Trigger multiple complaint submissions and verify individual WorkManager jobs are created (Logcat inspection).
