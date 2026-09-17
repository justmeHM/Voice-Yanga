# Voice Yanga Android App–API Integration Guide

Use this guide to connect the native Java Android app to the Voice Yanga API.

**API base path:** `/api/v1`  
**Verified:** 67 registered operations; 115/115 API tests passing  
**Rule:** the app calls the API only. Never connect the app directly to PostgreSQL or include a Supabase service-role key.

## 1. Confirm the API is reachable

The deployed API must pass:

```text
GET https://<api-domain>/health  -> 200
GET https://<api-domain>/ready   -> 200 { "status": "ready" }
```

Use HTTPS in production. For a physical device on the development network, use the computer's LAN address instead of `localhost`.

## 2. Configure the Android project

Add permissions to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Add Retrofit and Gson conversion to `app/build.gradle`:

```gradle
dependencies {
    implementation "com.squareup.retrofit2:retrofit:3.0.0"
    implementation "com.squareup.retrofit2:converter-gson:3.0.0"
}
```

Define the URL per build type. It must end with `/`:

```gradle
android {
    buildFeatures {
        buildConfig true
    }

    buildTypes {
        debug {
            buildConfigField "String", "API_BASE_URL",
                '"http://192.168.1.20:3000/api/v1/"'
            buildConfigField "String", "API_ORIGIN",
                '"http://192.168.1.20:3000"'
        }

        release {
            buildConfigField "String", "API_BASE_URL",
                '"https://<api-domain>/api/v1/"'
            buildConfigField "String", "API_ORIGIN",
                '"https://<api-domain>"'
        }
    }
}
```

Never add `DATABASE_URL`, `DIRECT_DATABASE_URL`, `JWT_SECRET`, or `SUPABASE_SERVICE_ROLE_KEY` to the Android project.

## 3. Create response and authentication models

```java
public class ApiEnvelope<T> {
    public boolean success;
    public T data;
    public String message;
}

public class AppUser {
    public String id;
    public String role;
    public String name;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public List<String> permissions;
    public String createdAt;
}

public class AuthResponse {
    public String token;
    public String refreshToken;
    public AppUser user;
}

public class RefreshData {
    public String accessToken;
    public String refreshToken;
}

public class LoginRequest {
    public String identifier;
    public String password;

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
}

public class RefreshRequest {
    public String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

public class RegisterRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String password;
}
```

## 4. Store the session securely

Create one abstraction backed by Android Keystore encryption or the secure credential storage already used by the app:

```java
public interface TokenStore {
    String getAccessToken();
    String getRefreshToken();
    AppUser getUser();

    void save(String accessToken, String refreshToken, AppUser user);
    void updateTokens(String accessToken, String refreshToken);
    void clear();
}
```

Do not save passwords or log tokens. Keep the access token, refresh token, and user together so role-based navigation uses the authenticated server response.

## 5. Define the Retrofit interface

```java
public interface VoiceYangaApi {

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("auth/refresh")
    Call<ApiEnvelope<RefreshData>> refresh(@Body RefreshRequest body);

    @POST("auth/logout")
    Call<ApiEnvelope<Object>> logout(@Body RefreshRequest body);

    @GET("users/profile")
    Call<AppUser> getProfile();

    @PATCH("users/profile")
    Call<ApiEnvelope<AppUser>> updateProfile(@Body Map<String, Object> body);

    @GET("categories")
    Call<ApiEnvelope<List<Category>>> getCategories();

    @GET("locations")
    Call<ApiEnvelope<List<Location>>> getLocations();

    @POST("complaints")
    Call<CreateComplaintResponse> createComplaint(@Body ComplaintRequest body);

    @Multipart
    @POST("complaints")
    Call<CreateComplaintResponse> createComplaintWithPhotos(
        @Part("title") RequestBody title,
        @Part("description") RequestBody description,
        @Part("category") RequestBody category,
        @Part("location") RequestBody location,
        @Part("priority") RequestBody priority,
        @Part("clientUuid") RequestBody clientUuid,
        @Part List<MultipartBody.Part> photos
    );

    @Multipart
    @POST("voice-notes")
    Call<ApiEnvelope<VoiceNoteData>> uploadVoiceNote(
        @Part MultipartBody.Part file,
        @Part("durationSeconds") RequestBody durationSeconds
    );

    @GET("voice-notes")
    Call<ApiEnvelope<List<VoiceNoteData>>> getPendingVoiceNotes();

    @GET("complaints/my")
    Call<ApiEnvelope<PaginatedComplaints>> getMyComplaints(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("complaints/{id}")
    Call<ApiEnvelope<Complaint>> getComplaint(@Path("id") String id);

    @GET("complaints/{id}/history")
    Call<ApiEnvelope<List<HistoryItem>>> getHistory(@Path("id") String id);

    @GET("complaints/{id}/comments")
    Call<List<Comment>> getComments(@Path("id") String id);

    @POST("complaints/{id}/comments")
    Call<ApiEnvelope<Comment>> addComment(
        @Path("id") String id,
        @Body CommentRequest body
    );

    @POST("complaints/{id}/support")
    Call<SupportResponse> support(@Path("id") String id);

    @GET("photos")
    Call<ApiEnvelope<List<ComplaintMedia>>> getPhotos(
        @Query("complaintId") String complaintId
    );

    @GET("notifications")
    Call<ApiEnvelope<PaginatedNotifications>> getNotifications(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("unreadOnly") boolean unreadOnly
    );

    @PATCH("notifications/{id}/read")
    Call<ApiEnvelope<Object>> markNotificationRead(@Path("id") String id);

    @PATCH("notifications/read-all")
    Call<ApiEnvelope<Object>> markAllNotificationsRead();
}
```

## 6. Create the authenticated API client

Add the access token to every authenticated request:

```java
public final class ApiClient {
    private static VoiceYangaApi api;
    private static OkHttpClient httpClient;

    public static synchronized VoiceYangaApi create(TokenStore tokenStore) {
        if (api != null) return api;

        Interceptor bearerInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder request = original.newBuilder()
                .header("Accept", "application/json");

            String token = tokenStore.getAccessToken();
            if (token != null && !token.isEmpty()) {
                request.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(request.build());
        };

        httpClient = new OkHttpClient.Builder()
            .addInterceptor(bearerInterceptor)
            .followRedirects(true)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        api = retrofit.create(VoiceYangaApi.class);
        return api;
    }

    public static OkHttpClient getHttpClient() {
        return httpClient;
    }

    private ApiClient() {}
}
```

When an endpoint returns 401:

1. Send `POST auth/refresh` with the saved refresh token.
2. Save both `data.accessToken` and `data.refreshToken`.
3. Retry the original request once.
4. If refresh fails, clear the session and return to Login.

Use one synchronized refresh operation so multiple 401 responses do not rotate the same refresh token concurrently.

## 7. Connect registration, login, and logout

Registration always creates a citizen and persists it in Supabase PostgreSQL:

```java
RegisterRequest body = new RegisterRequest();
body.firstName = firstName;
body.lastName = lastName;
body.email = email;
body.phone = phone;
body.password = password;

api.register(body).enqueue(new Callback<AuthResponse>() {
    @Override
    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            AuthResponse auth = response.body();
            tokenStore.save(auth.token, auth.refreshToken, auth.user);
            openHomeScreen();
        } else {
            showApiError(response);
        }
    }

    @Override
    public void onFailure(Call<AuthResponse> call, Throwable error) {
        showNetworkError();
    }
});
```

Login:

```java
api.login(new LoginRequest(identifier, password)).enqueue(/* same handling */);
```

Logout:

```java
String refreshToken = tokenStore.getRefreshToken();
api.logout(new RefreshRequest(refreshToken)).enqueue(/* clear local session */);
```

Registration requirements:

- First and last name are required.
- Email or phone is required.
- Password must be at least eight characters and contain a number.
- Email and phone must be unique.

## 8. Load categories and locations

Call these when the Report Issue screen opens:

```java
api.getCategories().enqueue(/* populate category spinner */);
api.getLocations().enqueue(/* populate location spinner */);
```

Do not hard-code these values. Complaint creation expects the selected names:

```java
String categoryValue = selectedCategory.name;

String locationValue;
if (selectedLocation.ward != null) {
    locationValue = selectedLocation.ward;
} else if (selectedLocation.constituency != null) {
    locationValue = selectedLocation.constituency;
} else {
    locationValue = selectedLocation.district;
}
```

Send the exact selected location text. That location controls which organization dashboard sees the complaint.

## 9. Submit a text complaint

```java
public class ComplaintRequest {
    public String title;
    public String description;
    public String category;
    public String location;
    public String priority;
    public String clientUuid;
    public String voiceNoteUrl;
    public Integer voiceNoteDurationSeconds;
}

public class CreateComplaintResponse {
    public String serverId;
    public String referenceCode;
    public String status;
    public String syncStatus;
}
```

Create `clientUuid` once when the local draft is created:

```java
ComplaintRequest body = new ComplaintRequest();
body.title = title;
body.description = description;
body.category = categoryValue;
body.location = locationValue;
body.priority = "MEDIUM";
body.clientUuid = UUID.randomUUID().toString();

api.createComplaint(body).enqueue(/* save response */);
```

Save `serverId`, `referenceCode`, `status`, and `syncStatus`. If the network fails, retry with the same `clientUuid`; do not generate a new one.

Rules:

- Title: 5–150 characters.
- Description: at least 10 characters unless a voice note is attached.
- Priority: `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.

## 10. Submit a complaint with photos

Copy a selected content `Uri` into the app cache before creating a multipart request:

```java
public static File copyToCache(Context context, Uri uri, String extension)
    throws IOException {

    File output = new File(
        context.getCacheDir(),
        UUID.randomUUID() + extension
    );

    try (InputStream input = context.getContentResolver().openInputStream(uri);
         OutputStream target = new FileOutputStream(output)) {

        if (input == null) throw new IOException("Cannot open selected file");

        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            target.write(buffer, 0, count);
        }
    }

    return output;
}
```

Create each photo part with the exact field name `photos`:

```java
String mime = contentResolver.getType(uri);
File file = copyToCache(context, uri, ".jpg");

RequestBody fileBody = RequestBody.create(
    MediaType.parse(mime != null ? mime : "image/jpeg"),
    file
);

MultipartBody.Part photo = MultipartBody.Part.createFormData(
    "photos",
    file.getName(),
    fileBody
);
```

Text multipart fields:

```java
MediaType text = MediaType.parse("text/plain");
RequestBody titlePart = RequestBody.create(text, title);
RequestBody descriptionPart = RequestBody.create(text, description);
RequestBody categoryPart = RequestBody.create(text, categoryValue);
RequestBody locationPart = RequestBody.create(text, locationValue);
RequestBody priorityPart = RequestBody.create(text, priority);
RequestBody uuidPart = RequestBody.create(text, clientUuid);
```

Then call `createComplaintWithPhotos(...)`.

Limits:

- Maximum five photos.
- JPEG, PNG, or WebP.
- Maximum 5 MiB per photo by default.

## 11. Record and submit a voice note

Request `RECORD_AUDIO` permission at runtime. Record an AAC audio track inside an MP4/M4A container:

```java
File audioFile = new File(
    getCacheDir(),
    UUID.randomUUID() + ".m4a"
);

MediaRecorder recorder = new MediaRecorder();
recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
recorder.setOutputFile(audioFile.getAbsolutePath());
recorder.prepare();
recorder.start();
```

Track the start time. When the user stops:

```java
recorder.stop();
recorder.release();
recorder = null;

int durationSeconds = (int) Math.ceil(
    (SystemClock.elapsedRealtime() - startedAt) / 1000.0
);
```

Upload the audio first:

```java
RequestBody audioBody = RequestBody.create(
    MediaType.parse("audio/mp4"),
    audioFile
);

MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
    "file",
    audioFile.getName(),
    audioBody
);

RequestBody durationPart = RequestBody.create(
    MediaType.parse("text/plain"),
    String.valueOf(durationSeconds)
);

api.uploadVoiceNote(audioPart, durationPart).enqueue(/* handle result */);
```

The response contains:

```java
public class VoiceNoteData {
    public String fileUrl;
    public int durationSeconds;
    public String mimeType;
}
```

Submit the complaint using the exact returned values:

```java
ComplaintRequest body = new ComplaintRequest();
body.title = title;
body.description = description != null ? description : "";
body.category = categoryValue;
body.location = locationValue;
body.priority = priority;
body.clientUuid = clientUuid;
body.voiceNoteUrl = uploaded.fileUrl;
body.voiceNoteDurationSeconds = uploaded.durationSeconds;

api.createComplaint(body).enqueue(/* save response */);
```

Voice-note limits are 120 seconds and 10 MiB by default. Pending uploads expire after 24 hours if no complaint attaches them.

## 12. Connect complaint screens

| Mobile screen/action | Retrofit call |
|---|---|
| Create Account | `register()` |
| Login | `login()` |
| Report Issue | `getCategories()`, `getLocations()`, then complaint create |
| My Complaints | `getMyComplaints(page, 20)` |
| Complaint Detail | `getComplaint(serverId)` |
| Timeline | `getHistory(serverId)` |
| Comments | `getComments()` and `addComment()` |
| Support | `support(serverId)` |
| Notifications | `getNotifications()` |
| Profile | `getProfile()` and `updateProfile()` |

Pagination is zero-based:

```java
public class PaginationMeta {
    public int totalItems;
    public int totalPages;
    public int page;
    public int limit;
}

public class PaginatedComplaints {
    public List<Complaint> data;
    public PaginationMeta meta;
}
```

Complaint status values are:

```text
SUBMITTED, REVIEWED, VERIFIED, ASSIGNED,
IN_PROGRESS, RESOLVED, CLOSED, REJECTED
```

If support returns `409 ALREADY_VOTED`, mark the item as already supported and do not retry.

## 13. Display private photos and play audio

Media records contain `/uploads/...` paths. Send the bearer token to that API path. The API validates access and redirects to a temporary Supabase signed URL.

Resolve it off the main thread:

```java
public static String resolveMediaUrl(
    String fileUrl,
    TokenStore tokenStore
) throws IOException {

    Request request = new Request.Builder()
        .url(BuildConfig.API_ORIGIN + fileUrl)
        .header("Authorization", "Bearer " + tokenStore.getAccessToken())
        .build();

    try (okhttp3.Response response =
             ApiClient.getHttpClient().newCall(request).execute()) {

        if (!response.isSuccessful()) {
            throw new IOException("Media request failed: " + response.code());
        }

        return response.request().url().toString();
    }
}
```

Pass the returned URL to the app's image loader or `MediaPlayer`. Do not save the signed URL permanently; save the stable `/uploads/...` path and resolve it again after expiry.

## 14. Implement offline complaint synchronization

Store unsent complaints in Room with at least:

```text
localId, clientUuid, title, description, category, location,
priority, photoPaths, voiceNotePath, syncStatus,
serverId, referenceCode
```

Use WorkManager with a connected-network constraint:

```java
Constraints constraints = new Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build();

OneTimeWorkRequest request =
    new OneTimeWorkRequest.Builder(ComplaintSyncWorker.class)
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            30,
            TimeUnit.SECONDS
        )
        .build();

WorkManager.getInstance(context).enqueueUniqueWork(
    "complaint-" + clientUuid,
    ExistingWorkPolicy.KEEP,
    request
);
```

The worker must reuse the stored `clientUuid`. It marks the local record `SYNCED` only after receiving `serverId` and `referenceCode`.

## 15. Connect notifications

Load and update notification state through the Retrofit methods above.

When Firebase gives the device a new FCM token:

```java
Map<String, Object> update = new HashMap<>();
update.put("fcmToken", fcmToken);
api.updateProfile(update).enqueue(/* handle response */);
```

Push delivery additionally requires `FCM_ENABLED=true` and Firebase credentials on the API server. In-app notifications remain available without external push delivery.

## 16. Parse API errors

```java
public class FieldError {
    public String field;
    public String message;
}

public class ApiError {
    public String timestamp;
    public int status;
    public boolean error;
    public String code;
    public String message;
    public String path;
    public List<FieldError> details;
}
```

Parse unsuccessful bodies with Gson and handle:

| Status | App behavior |
|---:|---|
| 400 | Display validation errors; do not retry unchanged data |
| 401 | Refresh once; log out if refresh fails |
| 403 | Show access denied and hide the unauthorized action |
| 404 | Show not found or refresh local data |
| 409 | Reconcile duplicate/conflicting state |
| 413 | Ask for a smaller image or recording |
| 429 | Honor `Retry-After` |
| 500/503 | Retain `clientUuid` and retry later |

Do not display raw error bodies or stack traces.

## 17. Verify organization visibility

The mobile app must submit an exact location selected from `GET /locations`. The complaint is visible on an organization dashboard when that organization has the same `locationId` and the staff user belongs to it.

Verification sequence:

1. Register a new citizen in the Android app.
2. Select a real API location.
3. Submit a complaint and save its `serverId`.
4. Log in to the dashboard as a matching-location organization user.
5. Confirm the complaint appears through `GET /complaints`.
6. Add a visible comment or valid status update.
7. Refresh the Android detail screen and confirm the update appears.
8. Confirm a different-location organization cannot access the complaint.

Organization self-registration is not currently implemented. An administrator must create organizations and organization staff accounts until that feature is added.

## 18. Final integration checklist

- [ ] Release build uses the deployed HTTPS API URL.
- [ ] `/health` and `/ready` return 200.
- [ ] Registration creates a real citizen who can log in after an API restart.
- [ ] Access and refresh tokens are stored securely.
- [ ] A 401 performs one synchronized refresh.
- [ ] Category and location selectors use API data.
- [ ] Text complaints persist and return `serverId` and `referenceCode`.
- [ ] Retrying the same `clientUuid` does not create a duplicate.
- [ ] Up to five supported photos upload and display.
- [ ] Voice recording uploads, attaches, and plays.
- [ ] My Complaints, detail, timeline, comments, support, and notifications work.
- [ ] Matching-location organization users see the complaint.
- [ ] Different-location organizations cannot see it.
- [ ] No server secret appears in the APK, source, or Logcat.

For exact deployed schemas, open:

```text
https://<api-domain>/api-docs/
https://<api-domain>/api-docs.json
```
