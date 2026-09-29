# Streamlining Complaint Reporting UI/UX

Improve the complaint reporting flow by introducing a visual category selector, a stepped navigation approach, and conditional visibility for the description field when a voice note is used.

## Proposed Changes

### Data Layer

#### [MODIFY] [CategoryDto.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/remote/dto/CategoryDto.java)
- Add `iconUrl` or `imageResName` field to support visual category selection.

### UI Resources

#### [NEW] [item_category.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/item_category.xml)
- Create a layout for a category card featuring an icon/image and a label.

#### [MODIFY] [activity_create_complaint.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/activity_create_complaint.xml)
- Replace the legacy category selector card with a `RecyclerView` for visual category selection.
- Reorganize the layout to support a "stepped" flow (e.g., hiding description/media until a category is selected).
- Add a toggle or selection for "Description Type" (Text vs. Voice).

### Presentation Layer

#### [NEW] [CategoryAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/CategoryAdapter.java)
- Implement a `RecyclerView.Adapter` to display categories with images.

#### [MODIFY] [CreateComplaintActivity.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/CreateComplaintActivity.java)
- Initialize the `CategoryAdapter`.
- Implement logic to reveal the description/voice note section after category selection.
- Add logic to hide `tilDescription` when a voice note is recorded or selected.
- Smoothly scroll to the next section upon category selection.

#### [MODIFY] [ComplaintViewModel.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/complaints/ComplaintViewModel.java)
- Ensure validation logic supports the "Description OR Voice Note" requirement (already partially present).

## Verification Plan

### Manual Verification
1.  **Visual Check:** Verify categories show images/icons in the new grid layout.
2.  **Flow Check:** Verify that selecting a category smoothly transitions the user to the description/voice note section.
3.  **Conditional Logic Check:** Record a voice note and verify the text description field is hidden. Delete the voice note and verify the text description field returns (or handle appropriately).
4.  **Submission Check:** Ensure a complaint can be submitted with only a voice note and category.
