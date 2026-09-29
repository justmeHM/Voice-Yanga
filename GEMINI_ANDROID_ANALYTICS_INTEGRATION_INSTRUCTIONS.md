# Gemini Android Studio Instructions: Voice Yanga Analytics Integration

## Objective

Integrate Voice Yanga mobile analytics into the real Android project without changing existing complaint, upload, authentication, navigation, or synchronization behavior.

The backend analytics ingestion endpoint already exists:

```text
POST /api/v1/analytics/events
```

Base URL follows the existing Android API configuration. Do not add a second HTTP client or bypass the existing Retrofit/OkHttp architecture.

## Scope lock

Change only:

- Android analytics event client/model code.
- Local analytics event queue and worker code, if required by the existing architecture.
- Focused analytics instrumentation at the existing app flow boundaries.
- Focused Android unit/instrumentation tests.

Do not change:

- Complaint request or response contracts.
- Complaint feed parsing, ordering, filters, or pagination.
- Authentication token refresh behavior.
- Complaint upload behavior.
- Room complaint schema unless the existing architecture requires a separate analytics queue table.
- Navigation or screen layout unrelated to emitting analytics events.
- Existing error handling for user-visible operations.
- Backend/API files.

If the Android source paths differ from the examples below, locate the equivalent existing classes and preserve the current architecture.

## Backend contract

### Request

```http
POST /api/v1/analytics/events
Content-Type: application/json
```

Authentication is optional. Anonymous pre-login events are allowed. If the existing authenticated client adds a Bearer token, it may be used, but analytics must still work when the user is signed out.

Example:

```json
{
  "eventName": "app_session_started",
  "schemaVersion": 1,
  "occurredAt": "2026-09-23T10:15:30.000Z",
  "anonymousUserId": "anonymous-citizen-uuid",
  "sessionId": "session-uuid",
  "platform": "ANDROID",
  "appVersion": "1.0.0",
  "requestId": "request-uuid",
  "idempotencyKey": "event-uuid",
  "properties": {
    "connectivity": "ONLINE"
  }
}
```

### Success

HTTP `202`:

```json
{
  "success": true,
  "data": {
    "accepted": true,
    "eventId": "server-event-uuid"
  }
}
```

### Retry duplicate

HTTP `409` for a previously accepted `idempotencyKey` means the event already exists. Mark that event as successfully synchronized and remove it from the local queue. Do not retry it forever.

### Validation failures

HTTP `400` means the event payload is invalid. Mark the event permanently rejected, remove it from the retry queue, and do not show a user-facing error.

### Temporary failures

For network errors, timeouts, HTTP `408`, `429`, and `5xx` responses:

- Keep the event locally.
- Retry with bounded exponential backoff.
- Respect connectivity constraints.
- Never block complaint submission or app navigation.

## Allowed event names

Only emit these exact lowercase snake_case names:

```text
app_session_started
registration_started
registration_completed
verification_completed
complaint_creation_started
complaint_draft_saved
complaint_submission_attempted
complaint_submission_failed
media_attachment_started
media_attachment_failed
complaint_viewed
complaint_supported
notification_opened
notification_action_completed
resolution_feedback_submitted
```

Do not invent event names in the Android app without updating the backend catalogue first.

## Required event fields

Create one Kotlin/Java model matching these fields:

```text
idempotencyKey: String
schemaVersion: Int = 1
eventName: String
occurredAt: String          // UTC ISO-8601
anonymousUserId: String?
sessionId: String?
platform: String            // ANDROID
appVersion: String?
locationId: String?
complaintId: String?
requestId: String?
properties: Map<String, Any?>
```

The server accepts anonymous events, so do not require an access token in the event producer.

Use a stable random anonymous identifier stored in the existing secure local-storage mechanism. Do not use email, phone number, access token, refresh token, or raw device identifiers as `anonymousUserId`.

Use a session ID that changes when a new app session starts according to the app's existing lifecycle conventions.

Read the app version from the existing build/package metadata. Do not hard-code it.

## Local queue requirements

Prefer the existing Room and WorkManager infrastructure. If no suitable queue exists, add a small separate analytics queue using the existing database and dependency-injection conventions.

Each queued event should store:

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

1. Enqueue locally before attempting network delivery.
2. Do not fail or delay a user action because analytics enqueueing fails.
3. Use a unique constraint on `idempotencyKey`.
4. Upload oldest due events first.
5. Batch only if the existing API client supports it; otherwise send one event per request.
6. Delete after `202` or `409`.
7. Retain transient failures with a maximum retry policy.
8. Drop malformed or permanently rejected events after recording a non-sensitive diagnostic.
9. Bound the queue size and delete the oldest low-priority events if storage limits are reached.
10. Never log payload JSON in production.

The analytics worker must be best-effort and must never interfere with complaint sync, media upload, token refresh, or logout.

## Instrumentation points

Use existing ViewModels, repositories, Activities, Fragments, Compose screens, and notification handlers. Do not put Retrofit calls directly in UI components.

### App lifecycle

Emit `app_session_started` once per session after the app becomes active. Include:

```text
platform: ANDROID
appVersion
connectivity: ONLINE | OFFLINE | UNKNOWN
```

Do not emit repeatedly on every Activity recreation.

### Registration and verification

Emit:

- `registration_started` when the registration flow opens.
- `registration_completed` only after the existing registration API succeeds.
- `verification_completed` only after the existing verification API succeeds.

Do not include email, phone, password, or verification tokens.

### Complaint creation

Emit:

- `complaint_creation_started` when a new complaint flow begins.
- `complaint_draft_saved` when the existing draft mechanism saves.
- `complaint_submission_attempted` immediately before the existing request.
- `complaint_submission_failed` only when the existing request fails.

For `complaint_submission_failed`, use a bounded class such as:

```text
NETWORK
TIMEOUT
VALIDATION
AUTHENTICATION
SERVER
UNKNOWN
```

Do not include complaint title, description, location text, or media paths.

### Complaint success

Do not emit `complaint_submitted` from the Android client. The backend emits the authoritative event after the transaction commits.

After a successful response, the app may record the existing server complaint ID in later behavioral events.

### Media

Emit:

- `media_attachment_started` when the user begins selecting or recording media.
- `media_attachment_failed` when the client-side attachment fails.

Use only bounded values:

```text
mediaType: PHOTO | VOICE_NOTE
failureClass: PERMISSION | SIZE | FORMAT | STORAGE | CANCELLED | UNKNOWN
```

Do not include file paths, filenames, media bytes, or audio contents.

### Complaint engagement

Emit:

- `complaint_viewed` when the user opens an issue detail screen.
- `complaint_supported` after the existing support request succeeds.

Required properties:

```text
viewContext: FEED | OWN_COMPLAINT | NOTIFICATION | DEEP_LINK | OTHER
supportState: SUPPORTED
```

Include only the server complaint ID when available. Never include complaint text.

### Notifications

Emit `notification_opened` only when the user opens an in-app or push notification. Emit `notification_action_completed` after the target action succeeds.

Use:

```text
channel: PUSH | IN_APP | EMAIL
notificationType: bounded existing notification type
actionType: bounded action identifier
```

Do not include notification message text or provider tokens.

### Resolution feedback

If the app already has a resolution-feedback flow, emit `resolution_feedback_submitted` only after the existing API confirms success.

Use a bounded rating or feedback type. Do not include free-form feedback text in analytics properties.

## Authentication and logout safety

- Analytics must not alter access-token refresh behavior.
- Analytics must not block logout.
- Clear or rotate the anonymous user identity only according to the existing account-deletion/privacy policy.
- Do not send an account-deleted user's old authenticated identity after deletion.
- Never log Authorization headers or tokens.

## Testing requirements

Add focused tests for:

1. Event serialization uses UTC ISO-8601 timestamps.
2. Anonymous user ID is stable across app restarts.
3. Session ID changes only at the intended session boundary.
4. Events are enqueued before delivery.
5. Successful `202` removes an event.
6. Duplicate `409` removes an event without repeated retries.
7. Network failure retains an event and schedules backoff.
8. `400` permanently rejects an event without blocking the user flow.
9. Queue uniqueness prevents duplicate idempotency keys.
10. Complaint submission still succeeds if analytics enqueue or delivery fails.
11. No forbidden fields appear in serialized properties.
12. Logout and account deletion do not leave credentials or sensitive analytics payloads behind.

## Manual validation checklist

- Launch the app offline and open registration: the app remains usable.
- Create and save a draft offline: the existing draft behavior is unchanged.
- Submit a complaint online: complaint creation succeeds even if analytics delivery is unavailable.
- Restore connectivity: queued events upload in order.
- Kill and restart the app: queued events are not duplicated.
- Open a notification: exactly one `notification_opened` event is queued.
- Sign out: no authenticated API behavior changes.
- Delete an account: no old account credentials or sensitive event data remains locally.

## Completion criteria

The implementation is complete only when:

- Existing Android tests remain green.
- Complaint creation, uploads, authentication, refresh, logout, and account deletion behavior are unchanged.
- Analytics delivery is best-effort and offline-safe.
- The backend accepts the emitted event names and payloads.
- Duplicate retries are safe.
- No secrets, personal contact details, complaint content, or private media data are sent.
- The Android project records the exact commit/files changed and the validation results.
