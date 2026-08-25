# Enterprise UX Improvements: Phase 4 (Next 5 Features)

Continue the transformation into a premium enterprise application with edge-to-edge aesthetics, automation, and privacy enhancements.

## User Review Required

> [!IMPORTANT]
> - **Edge-to-Edge**: The app UI will now flow behind the status and navigation bars for a more immersive look.
> - **Category Suggester**: The app will automatically suggest a category based on the words in your title (e.g., typing "Water" will auto-select the Water category).
> - **Privacy**: GPS metadata (EXIF) will be stripped from photos before they leave your device.

## Proposed Changes

### 1. Edge-to-Edge Design (UX Improvement #4)
- **Activity Logic**: Update `BaseActivity` (or all main activities) to use `enableEdgeToEdge()` and apply proper window insets (padding) to prevent UI overlap with system bars.
- **Styling**: Update themes to make status and navigation bars transparent.

### 2. Keyword-Based Category Suggester (UX Improvement #9)
- **Logic**: Implement a local keyword mapper in `CreateComplaintActivity` (e.g., "pipe", "leak" -> Water; "pothole", "street" -> Roads).
- **Automation**: Listen to title input and automatically select the matching category card if a strong match is found.

### 3. Exif Data Stripping (UX Improvement #8)
- **Utility**: Update `ImageCompressor` to strip metadata tags (especially GPS location and device info) from the Bitmap before saving the compressed JPEG.

### 4. Detailed Error Views (UX Improvement #19)
- **UI**: Create a reusable `LayoutErrorBinding` with a "Retry" button.
- **Integration**: Replace empty white screens with this layout in Home and Notifications when the initial network fetch fails.

### 5. Dynamic App Shortcuts (UX Improvement #20)
- **Shortcuts**: Add "Report a Problem" as a long-press shortcut on the app icon.
- **Deep Linking**: Ensure it opens `CreateComplaintActivity` directly.

## Verification Plan

### Automated Tests
- `gradle_build`: Verify resource linking for new error layouts.

### Manual Verification
- **Edge-to-Edge**: Verify the background color flows all the way to the top and bottom of the screen.
- **Suggester**: Type "Broken water pipe" in the title; verify the "Water" category is automatically highlighted.
- **Privacy**: Upload a photo; verify the server copy has no EXIF location data.
- **Shortcuts**: Long-press the app icon on the Android home screen and tap "Report a Problem."
