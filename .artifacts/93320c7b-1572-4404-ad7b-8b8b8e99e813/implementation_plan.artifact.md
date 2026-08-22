# Iteration 5 — Complaint Management & Community Engagement

This iteration implements Sprint 4 requirements, focusing on the citizen's ability to view their own complaints, see official/community comments, and view attached photos in the detail screen. It also improves traceability and handles the "My Complaints" filtering logic.

## User Review Required

> [!IMPORTANT]
> - **My Complaints Filtering:** We are using `authorEmail` from the session to filter complaints locally. In a production environment, this would ideally be a unique user ID from the backend.
> - **UI Changes:** I'm adding "My Complaints" to the Navigation Drawer as it's the current primary navigation pattern in the app, rather than switching to a Bottom Navigation as suggested by the SDLC (to maintain consistency with the current implementation).

## Proposed Changes

### Data Layer
#### [MODIFY] [Complaint.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/entity/Complaint.java)
- Add `authorEmail` field to associate complaints with the current user.
- Update constructor and getters/setters.

#### [MODIFY] [ComplaintDao.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/dao/ComplaintDao.java)
- Add `getMyComplaints(String email)` to fetch only user-specific reports.

#### [MODIFY] [ComplaintRepository.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/repository/ComplaintRepository.java)
- Update `saveComplaint` to populate `authorEmail` from `SessionManager`.

### Home & Navigation
#### [MODIFY] [nav_drawer_menu.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/menu/nav_drawer_menu.xml)
- Add "My Complaints" menu item `[FR-COMP-05]`.

#### [MODIFY] [HomeActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/HomeActivity.java)
- Handle "My Complaints" navigation.

### My Complaints Feature
#### [NEW] [MyComplaintsActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/MyComplaintsActivity.java)
- Activity to display user's own reports.

#### [NEW] [MyComplaintsViewModel.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/MyComplaintsViewModel.java)
- ViewModel for filtering complaints by session email.

#### [NEW] [activity_my_complaints.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/activity_my_complaints.xml)
- Layout for the My Complaints screen.

### Complaint Detail Enhancements
#### [MODIFY] [activity_complaint_detail.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/activity_complaint_detail.xml)
- Add `RecyclerView` for photos (horizontal).
- Add "Official Updates & Comments" section with a `RecyclerView` for comments.

#### [MODIFY] [ComplaintDetailActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintDetailActivity.java)
- Observe and display photos `[FR-COMP-03]`.
- Observe and display comments `[FR-COMP-06]`.

#### [NEW] [CommentAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/CommentAdapter.java)
- Adapter for displaying citizen-visible comments.

#### [NEW] [item_comment.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/item_comment.xml)
- Layout for individual comment cards, differentiating official updates.

## Verification Plan

### Automated Tests
- **Unit Test:** `ComplaintDaoTest` to verify filtering by `authorEmail`.
- **ViewModel Test:** `MyComplaintsViewModelTest` to ensure correct data retrieval.

### Manual Verification
- **My Complaints:** Log in, create a complaint, verify it appears in "My Complaints" but not if logged in as someone else (mocked).
- **Comments:** View a complaint detail and verify the mock "Official Admin" comment appears.
- **Photos:** Verify that photos attached during creation are visible in the detail screen.
