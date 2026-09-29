# Gemini Android App-Side Remaining Implementation Guide

## Objective

Implement the remaining Android-side analytics work that the backend contract now supports. The API side is already complete in this repo. The remaining work is in the real Android app repository and must be done without breaking complaint flow, auth flow, uploads, or app navigation.

## Backend status

The backend is already ready for these analytics events:

- `account_registered`
- `login_succeeded`
- `account_deleted`
- `app_session_started`
- `registration_started`
- `registration_completed`
- `verification_completed`
- `complaint_creation_started`
- `complaint_draft_saved`
- `complaint_submission_attempted`
- `complaint_submission_failed`
- `media_attachment_started`
- `media_attachment_failed`
- `complaint_viewed`
- `complaint_supported`
- `notification_opened`
- `notification_action_completed`
- `resolution_feedback_submitted`

The Android project must now implement the app-side emission and queueing for those events.

## Hard constraints

Do not change any of the following:

- complaint request/response contracts
- upload flow, retry behavior, or sync logic
- auth token refresh or logout flow
- navigation, screen flows, or layout behavior
- complaint text, media path, or user content payloads
- backend code or API contract files
- any app logic that is not explicitly analytics-related

Analytics must be best-effort. If analytics fails, the user-facing app must continue working normally.

## Required Android work

### 1) Event model and network payload

Create a single analytics event model that matches the API contract.

Required fields:

```kotlin
data class AnalyticsEventRequest(
    val idempotencyKey: String,
    val schemaVersion: Int = 1,
    val eventName: String,
    val occurredAt: String, // UTC ISO-8601
    val anonymousUserId: String?,
    val sessionId: String?,
    val platform: String = "ANDROID",
    val appVersion: String?,
    val locationId: String?,
    val complaintId: String?,
    val requestId: String?,
    val properties: Map<String, Any?>
)
```

Implementation notes:

- Serialize timestamps as UTC ISO-8601.
- Use a stable anonymous ID stored in secure local storage.
- Use a session ID that changes only when a new app session begins.
- Do not put email, password, tokens, device IDs, complaint text, or media paths in analytics payloads.
- Do not invent any event names beyond the approved list.

### 2) Local analytics queue

Add a small best-effort queue using the project’s existing persistence pattern.

Queue entry fields:

```text
localId
idempotencyKey
payloadJson
createdAt
attemptCount
nextAttemptAt
lastErrorClass
```

Rules:

- Enqueue before attempting delivery.
- Use a unique constraint on `idempotencyKey`.
- Remove event on HTTP `202` or `409`.
- Keep retrying on network errors, timeouts, `408`, `429`, and `5xx` with bounded backoff.
- Permanently reject `400` responses and drop them without user-visible error.
- Maintain a bounded queue size and evict oldest low-priority entries if needed.
- Never log raw payload JSON in production builds.

### 3) Worker / dispatcher

Implement a background worker that:

- pulls oldest due events first
- sends one event per request unless the project already batches safely
- retries with exponential backoff
- stops blocking app workflow
- runs independent of complaint sync and auth flows

This should be wired through the existing background architecture (WorkManager, coroutine worker, Room DAO, or the equivalent already used by the app).

### 4) App session tracking

Emit `app_session_started` once per app session after the app becomes active.

Required properties:

```text
connectivity: ONLINE | OFFLINE | UNKNOWN
```

Do not emit repeatedly during Activity recreation.

### 5) Registration and verification events

Emit:

- `registration_started` when the registration flow begins
- `registration_completed` only after the existing registration API returns success
- `verification_completed` only after the existing verification API returns success

Do not include:

- email
- phone
- password
- verification code
- tokens

### 6) Complaint creation and draft lifecycle

Emit:

- `complaint_creation_started` when the complaint creation flow starts
- `complaint_draft_saved` when the existing draft save flow succeeds
- `complaint_submission_attempted` immediately before the complaint POST request
- `complaint_submission_failed` only when the request fails

Use bounded failure classes:

```text
NETWORK
TIMEOUT
VALIDATION
AUTHENTICATION
SERVER
UNKNOWN
```

Do not send complaint title, description, location text, or media paths.

Important: do not emit `complaint_submitted` from Android. The backend emits the authoritative event when the transaction succeeds.

### 7) Media attachment events

Emit:

- `media_attachment_started` when the user begins selecting or recording media
- `media_attachment_failed` when the media attachment step fails

Use bounded values:

```text
mediaType: PHOTO | VOICE_NOTE
failureClass: PERMISSION | SIZE | FORMAT | STORAGE | CANCELLED | UNKNOWN
```

Never include file paths, filenames, media bytes, or raw audio.

### 8) Complaint engagement and support

Emit:

- `complaint_viewed` when a complaint detail screen is opened
- `complaint_supported` after support succeeds

Required properties:

```text
viewContext: FEED | OWN_COMPLAINT | NOTIFICATION | DEEP_LINK | OTHER
supportState: SUPPORTED
```

Only include the complaint ID if the app already has the server complaint ID available. Never include complaint text.

### 9) Notification events

Emit:

- `notification_opened` when a notification is opened by the user
- `notification_action_completed` after the target action succeeds

Use bounded values:

```text
channel: PUSH | IN_APP | EMAIL
notificationType: existing notification type
actionType: bounded action identifier
```

Never include message text, provider tokens, or user data.

### 10) Resolution feedback

If the app has a resolution feedback flow, emit:

- `resolution_feedback_submitted` only after the server confirms success

Use bounded rating or category values. Do not include free-form feedback text.

## Authentication and privacy rules

- Do not change token refresh or logout logic.
- Do not block logout or account deletion with analytics.
- Clear/rotate anonymous identity only according to the privacy policy.
- Do not send an old authenticated identity after account deletion.
- Never log Authorization headers or tokens.
- Never include sensitive personal fields in analytics payloads.

## Implementation priority

Implement in this order:

1. event model + payload serializer
2. local queue + unique idempotency key logic
3. background dispatcher + retry/backoff
4. app session event
5. registration and verification events
6. complaint creation and failure events
7. media events
8. complaint view/support events
9. notification events
10. resolution feedback

## Testing requirements

Add focused tests for:

1. UTC ISO-8601 timestamp serialization
2. stable anonymous ID across restarts
3. session ID change only at session boundary
4. events are queued before delivery
5. `202` removes queued event
6. `409` removes duplicate event without repeat retry
7. network failure retains event and schedules backoff
8. `400` permanently rejects event without blocking the user flow
9. duplicate idempotency key prevention
10. complaint submission still succeeds when analytics fails
11. no forbidden fields are present in payloads
12. logout and account deletion leave no sensitive state behind

## Manual validation checklist

- launch app offline and open registration; app stays usable
- save complaint draft offline; existing behavior unchanged
- submit complaint online; complaint flow still works if analytics fails
- restore connectivity; queued events upload in order
- kill and restart app; queue is not duplicated
- open notification; exactly one notification event is sent
- sign out; no auth behavior changes
- delete account; no old session or data remains in analytics queue

## Completion criteria

The Android implementation is complete only when:

- existing app tests remain green
- complaint, auth, upload, and account-deletion behavior are unchanged
- analytics is best-effort and offline-safe
- backend accepts the event names and payloads
- retries are safe and deduplicated
- no secret, contact, complaint-content, or media data is sent
- the Android repo records exact changed files and validation results
