# Voice Yanga: Firebase Push Notification Integration

This guide connects Firebase Cloud Messaging (FCM) to the existing Voice Yanga API and Java Android app. Supabase continues storing users, complaints, notification history, and media.

FCM delivery is free. SMTP is only needed for email delivery and is not required here. Other hosting and database services retain their own pricing and limits. See [Firebase pricing](https://firebase.google.com/pricing).

## 1. Create or select the Firebase project

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Create a project named **Voice Yanga**, or select the project already connected to the Android app.
3. Google Analytics is optional for basic push delivery.
4. Use the same Firebase project for the Android configuration and API credentials below.

## 2. Connect the Android app

In Android Studio, find the actual `applicationId` in `app/build.gradle` or `app/build.gradle.kts`.

```groovy
defaultConfig {
    applicationId "com.example.voiceyanga" // Example only: use your real ID.
}
```

In Firebase, choose **Add app > Android**, enter that application ID exactly, and register the app. Download `google-services.json` into the Android app module:

```text
YourAndroidProject/
  app/
    build.gradle
    google-services.json
```

Follow the Firebase console's Gradle instructions. Add the Google Services plugin and Firebase Messaging SDK; keep existing Android plugins and dependencies.

For a Groovy `build.gradle` project, add this to the project-level `plugins` block:

```groovy
id 'com.google.gms.google-services' version '4.5.0' apply false
```

In the app-level `build.gradle`, add:

```groovy
plugins {
    // Keep existing plugins here.
    id 'com.google.gms.google-services'
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:34.19.0')
    implementation 'com.google.firebase:firebase-messaging'
}
```

These versions were shown by Firebase's setup documentation when this guide was prepared. If Firebase is already installed, use the project's compatible BoM and avoid duplicate declarations. For `.gradle.kts`, use the Kotlin DSL instructions even though the app is written in Java. Click **Sync Now**.

Reference: [Firebase Android setup](https://firebase.google.com/docs/android/setup).

## 3. Download server credentials

1. In the same Firebase project, open **Project settings > Service accounts > Firebase Admin SDK**.
2. Select **Generate new private key** and download the JSON file.
3. Save it outside the API repository, for example:

```text
C:/Users/SYDNEY MULANDO/Secrets/voice-yanga-firebase-admin.json
```

4. Under **Project settings > Cloud Messaging**, check that **Firebase Cloud Messaging API (HTTP v1)** is enabled. If necessary, use the linked Google Cloud console to enable it for this project.

The downloaded Admin SDK JSON is a private server credential. Do not put it in the Android app, dashboard, Git, or chat. It is different from Android's `google-services.json`.

Reference: [Firebase Admin SDK setup](https://firebase.google.com/docs/admin/setup).

## 4. Configure the existing API

Add or update these entries in the API project's `.env`. Substitute the actual private-key file path:

```dotenv
FCM_ENABLED=true
NOTIFICATION_WORKER_ENABLED=true
GOOGLE_APPLICATION_CREDENTIALS="C:/Users/SYDNEY MULANDO/Secrets/voice-yanga-firebase-admin.json"
```

The API already includes `firebase-admin`, uses Application Default Credentials, and starts the notification worker with the server. It does not need another Firebase SDK installation.

Restart the API from its directory:

```powershell
Set-Location -LiteralPath 'D:\TECH DOME\PROJECTS\Heuristics Technologies\Voice Yanga API'
npm run dev
```

Keep the API running during tests. For deployment, securely provision the credential on the actual API server and set the environment variables there. A local Windows path cannot be used on another machine.

SMTP can remain unconfigured; set the testing user's notification preference `email` to `false` in step 6.

## 5. Configure Android permission and notification channel

Ensure these permissions appear outside `<application>` in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Request permission from an Activity on Android 13 or later when the user enables notifications:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

    ActivityCompat.requestPermissions(
        this,
        new String[]{Manifest.permission.POST_NOTIFICATIONS},
        1001
    );
}
```

Use Android Studio to import `android.Manifest`, `android.os.Build`, `android.content.pm.PackageManager`, and the AndroidX `ActivityCompat` and `ContextCompat` classes. Compile against API 33 or later for these constants. Handle the permission result; permission requests are asynchronous.

Create a channel at app startup for Android 8 or later:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    NotificationChannel channel = new NotificationChannel(
        "complaint_updates",
        "Complaint updates",
        NotificationManager.IMPORTANCE_DEFAULT
    );
    channel.setDescription("Responses and updates to your complaints");
    getSystemService(NotificationManager.class)
        .createNotificationChannel(channel);
}
```

Import `android.app.NotificationChannel` and `android.app.NotificationManager`. Inside the manifest's `<application>`, set the FCM default channel:

```xml
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="complaint_updates" />
```

Use `complaint_updates` for local foreground notifications too. Configure a suitable small notification icon in your Android resources.

Reference: [FCM Android setup](https://firebase.google.com/docs/cloud-messaging/android/get-started).

## 6. Register the phone and enable push for the user

After successful Voice Yanga login, retrieve the FCM registration token:

```java
FirebaseMessaging.getInstance().getToken()
    .addOnCompleteListener(task -> {
        if (!task.isSuccessful()) {
            // Schedule another synchronization attempt later.
            return;
        }

        String fcmToken = task.getResult();
        // Send the profile request below using your authenticated API client.
    });
```

Import `com.google.firebase.messaging.FirebaseMessaging`. The HTTP call in the comment must be implemented with the app's existing Retrofit, Volley, or other HTTP client. These snippets are integration examples, not a complete Android implementation.

Send the token using the logged-in user's Voice Yanga access token:

```http
PATCH /api/v1/users/profile
Authorization: Bearer <VOICE_YANGA_ACCESS_TOKEN>
Content-Type: application/json
```

```json
{
  "fcmToken": "<TOKEN_RETURNED_BY_FIREBASE>"
}
```

After the user opts in, enable their API notification preferences:

```http
PATCH /api/v1/notifications/preferences
Authorization: Bearer <VOICE_YANGA_ACCESS_TOKEN>
Content-Type: application/json
```

```json
{
  "inApp": true,
  "push": true,
  "email": false,
  "assignmentUpdates": true,
  "statusUpdates": true
}
```

Both requests should return HTTP 200. Saving the device token alone does not enable push: users without configured preferences default to `push: false`.

Use the existing API base URL. If it already ends in `/api/v1`, append `/users/profile` or `/notifications/preferences` only. The device must be able to reach that API address.

Synchronize the token after login and whenever it changes. Retry failed synchronization through your existing background-work mechanism. Preserve the user's preference choices; do not switch push back on automatically at every login.

## 7. Handle tokens and incoming messages in Java

Create `VoiceYangaMessagingService.java` in the app package:

```java
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class VoiceYangaMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // Save the latest token locally.
        // If signed in, synchronize it through PATCH /users/profile.
        // Otherwise synchronize it after login.
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String complaintId = message.getData().get("complaintId");
        String eventType = message.getData().get("type");
        RemoteMessage.Notification notification = message.getNotification();
        String title = notification != null
            ? notification.getTitle() : "Voice Yanga";
        String body = notification != null
            ? notification.getBody() : "You have a complaint update.";

        // Display a local notification using title/body when allowed.
        // Attach complaintId to its PendingIntent.
        // Refresh relevant screens using the API.
    }
}
```

Register the service inside `<application>` in `AndroidManifest.xml`. Adjust the class name if you place it in a subpackage:

```xml
<service
    android:name=".VoiceYangaMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Complete the commented integration points:

- Build foreground alerts with `NotificationCompat.Builder`, the channel from step 5, a valid small icon, and an immutable `PendingIntent` that opens your app with `complaintId`.
- Check notification permission before posting. Use distinct notification identifiers so unrelated complaint alerts do not overwrite each other.
- For background notification messages, Android normally displays the alert automatically. Read `complaintId` from the launcher Activity's Intent when the notification is tapped. Handle both `onCreate()` and `onNewIntent()`.
- If login is required, retain the requested complaint ID through login. Fetch `GET /api/v1/complaints/{id}` before displaying the complaint, and handle revoked access or missing records.
- Refresh `GET /api/v1/notifications` to update the in-app notification list. Do not treat the push message as the authoritative complaint record.

Reference: [Receive FCM messages on Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).

## 8. Test Firebase delivery

1. Install and open the updated app on a phone with Google Play services or a suitable emulator.
2. Sign in, grant notification permission, and complete step 6.
3. Obtain the current device token using a local debugger during development.
4. Open Firebase's Messaging section and create a notification test.
5. Use **Send test message**, enter that token, and put the app in the background.
6. Confirm the phone displays the test notification.

This verifies Firebase-to-device delivery. It does not verify the API's credentials or worker. Continue with the next test.

## 9. Test the complete Voice Yanga flow

1. Keep the API running with Firebase configured.
2. Submit a new complaint from the citizen account.
3. Open the responsible organization's dashboard.
4. Add a **public response** to the complaint.
5. Confirm that the citizen receives the phone notification.
6. Tap it and confirm that the app opens the correct complaint and displays the response.
7. Repeat while the app is in the foreground.
8. Verify that an internal organization note does not alert the citizen.

To check saved notifications independently of push delivery:

```http
GET /api/v1/notifications
Authorization: Bearer <VOICE_YANGA_ACCESS_TOKEN>
```

Expected event types include `COMPLAINT_SUBMITTED`, `COMPLAINT_ASSIGNED`, `COMPLAINT_STATUS_CHANGED`, and `COMPLAINT_COMMENT_ADDED`.

## 10. Troubleshooting and current limitations

| Symptom | Check |
|---|---|
| Firebase Console test does not arrive | Current device token, matching Firebase project, internet, Google Play services, Android permission, notification channel settings. |
| Console test works but API events do not arrive | API restarted, readable server credential, `FCM_ENABLED=true`, worker enabled, token stored for the correct user, `push=true`. |
| Public response is in the notification list but no phone alert appears | Check the PUSH delivery record and user preference; in-app storage and external delivery are separate. |
| Alert works in the background but not the foreground | Implement local notification display in `onMessageReceived()`. |
| Tapping an alert opens the app but not the complaint | Handle the `complaintId` Intent extra in both Activity creation and reuse, including after login. |
| Notifications go to another phone | The current API stores one FCM token per user; a new token replaces the old one. |

For backend diagnosis, inspect the relevant `DomainEvent` delivery record's `deliveryStatus`, `providerReference`, and `lastError`. A record marked `DELIVERED` with a `SKIPPED_...` provider reference was skipped, not sent to the phone.

Test with a new event after configuring Firebase. Previously failed or skipped deliveries are not automatically replayed by changing environment variables.

Before supporting shared phones or account switching in production, implement and verify device-token removal on logout. The current profile schema accepts a string token but does not provide a dedicated device-registration lifecycle or multi-device registration API. Disabling one user's push preference is account-wide and is not a substitute for removing a specific device registration.

## Completion checklist

- [ ] Android configuration and API credentials use the same Firebase project.
- [ ] Android Gradle sync/build succeeds with Firebase Messaging installed.
- [ ] Server private key is outside the repository and readable by the API process.
- [ ] API restarted with FCM and its notification worker enabled.
- [ ] Android permission granted and notification channel created.
- [ ] Current device token saved through the authenticated profile endpoint.
- [ ] User opted in and API push preference is enabled.
- [ ] Token refresh, foreground display, and notification-tap handling implemented.
- [ ] Firebase Console test received.
- [ ] Organization public-response notification received and opens the correct complaint.
- [ ] Internal notes do not notify the citizen.
- [ ] Logout/account-switch token handling reviewed before shared-device release.
