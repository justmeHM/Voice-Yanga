# Final UX Polish & Stability Plan

Fix critical navigation issues, stabilize the biometric flow, and ensure all assignment data is visible.

## User Review Required

> [!IMPORTANT]
> - **Swipe Sensitivity**: I've adjusted the gesture detection logic to prevent accidental "forced" redirects. Swipes now require a clear horizontal motion.
> - **Biometric Fix**: Corrected the initialization logic to ensure Fingerprint/Face ID works correctly across all device states.
> - **Assignment Data**: The "Assigned" status now correctly pulls and displays the organization name from the database.

## Proposed Changes

### 1. Robust Swipe Navigation

#### [MODIFY] [HomeActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/HomeActivity.java)
- Update `onFling` logic to compare `diffX` vs `diffY`. This ensures vertical scrolling doesn't trigger a horizontal activity transition.
- Use `slide_in_left` and `slide_out_right` for natural "push" navigation to the My Complaints page.

#### [MODIFY] [MyComplaintsActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/MyComplaintsActivity.java)
- Update `onFling` logic to detect Left Swipes (Right-to-Left) and return Home with a "pop" animation.

### 2. Biometric & Splash Stability

#### [MODIFY] [SplashActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/auth/SplashActivity.java)
- Ensure Intent extras (like deep links) are preserved through the biometric authentication loop.
- Fix null-pointer risks in the `sessionManager` check.

#### [MODIFY] [ProfileViewModel.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/profile/ProfileViewModel.java)
- Robust string splitting for full name to prevent crashes if the name has extra spaces or is just one word.

### 3. Assignment Visibility

#### [MODIFY] [TimelineAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/TimelineAdapter.java)
- Update `bind()` to show `subLabel` text (e.g., "Assigned (Water Dept)") in a secondary font style.

#### [MODIFY] [ComplaintDetailActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintDetailActivity.java)
- Pass the `assignedTo` field to the timeline builder.

## Verification Plan

### Automated Tests
- `gradle_build`: Verify no regression in syntax or resource IDs.

### Manual Verification
- **Gestures**: Scroll the feed vertically; verify it *doesn't* redirect you. Swipe Right deliberately; verify smooth transition.
- **Biometrics**: Enable in Profile, close app, and re-launch.
- **Timeline**: View an issue marked "ASSIGNED" and confirm the organization name is visible.
