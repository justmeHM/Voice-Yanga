# Enhance Trending Section & Smart Media Handling for Complaints

## Problem Description
1. **Trending Feed Accuracy**: Newly posted complaints with zero supports and zero comments appear in the Trending section, defeating the purpose of "Trending".
2. **Missing Media & Video Thumbnails in Trending**: Complaints with video attachments or text-only complaints without photos display blank/broken image containers in the Trending cards.

## Proposed Solutions & Recommendations

### 1. Trending Engagement Threshold Filter
- Update `ComplaintDao.getTrendingComplaints(...)` to enforce a minimum engagement threshold: `(supportCount > 0 OR commentCount > 0)`. Zero-support/comment posts will no longer clutter the trending feed.

### 2. Smart Media Handling in `TrendingAdapter` & Layout
- **Video Complaints**: Prioritize loading `videoThumbnailUrl` or extracting a video frame from `videoUrl` using Glide, and overlay a video play badge.
- **Text-Only Complaints (No Photo/Video)**: Automatically hide the `ivPhoto` ImageView (`View.GONE`) so text-only posts display as clean, well-spaced card snippets without empty placeholders.

## Proposed Changes

### [ComplaintDao.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/data/local/dao/ComplaintDao.java)
- Add `(supportCount > 0 OR commentCount > 0)` requirement to `getTrendingComplaints` query.

### [TrendingAdapter.java](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/java/com/voiceyanga/citizen/feature/home/TrendingAdapter.java) & [item_trending_complaint.xml](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/layout/item_trending_complaint.xml)
- Handle media presence: show thumbnail for photos/videos (with video play badge), or hide image container entirely for text-only complaints.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure clean compilation.

### Manual Verification
- Verify trending section only displays posts with supports/comments.
- Verify video complaints show video thumbnails/badges, and text-only complaints display cleanly without empty photo boxes.
