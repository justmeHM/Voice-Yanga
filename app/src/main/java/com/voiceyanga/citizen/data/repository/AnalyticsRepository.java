package com.voiceyanga.citizen.data.repository;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.dao.AnalyticsEventDao;
import com.voiceyanga.citizen.data.local.entity.AnalyticsEventEntity;
import com.voiceyanga.citizen.data.remote.AnalyticsSyncWorker;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.AnalyticsEvent;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AnalyticsRepository {

    private static final String TAG = "AnalyticsRepository";
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int DELETE_BATCH_SIZE = 100;

    private static final Set<String> ALLOWED_EVENTS = new HashSet<>(Arrays.asList(
            "account_registered",
            "login_succeeded",
            "account_deleted",
            "app_session_started",
            "registration_started",
            "registration_completed",
            "verification_completed",
            "complaint_creation_started",
            "complaint_draft_saved",
            "complaint_submission_attempted",
            "complaint_submission_failed",
            "media_attachment_started",
            "media_attachment_failed",
            "complaint_viewed",
            "complaint_supported",
            "notification_opened",
            "notification_action_completed",
            "resolution_feedback_submitted"
    ));

    private final Context context;
    private final AnalyticsEventDao analyticsEventDao;
    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final WorkManager workManager;
    private final ExecutorService executorService;
    private final Gson gson;

    private boolean sessionStartedInAppLifecycle = false;

    @Inject
    public AnalyticsRepository(
            @ApplicationContext Context context,
            AnalyticsEventDao analyticsEventDao,
            ApiService apiService,
            SessionManager sessionManager,
            WorkManager workManager) {
        this.context = context;
        this.analyticsEventDao = analyticsEventDao;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.workManager = workManager;
        this.executorService = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    public void trackAppSessionStarted() {
        if (sessionStartedInAppLifecycle) {
            return;
        }
        sessionStartedInAppLifecycle = true;
        
        Map<String, Object> props = new HashMap<>();
        props.put("platform", "ANDROID");
        props.put("appVersion", getAppVersion());
        props.put("connectivity", getConnectivityStatus());

        enqueueEvent("app_session_started", props, null, null, null);
    }

    public void trackRegistrationStarted() {
        enqueueEvent("registration_started", null, null, null, null);
    }

    public void trackRegistrationCompleted() {
        enqueueEvent("registration_completed", null, null, null, null);
    }

    public void trackAccountRegistered() {
        enqueueEvent("account_registered", null, null, null, null);
    }

    public void trackLoginSucceeded() {
        enqueueEvent("login_succeeded", null, null, null, null);
    }

    public void trackAccountDeleted() {
        enqueueEvent("account_deleted", null, null, null, null);
    }

    public void trackVerificationCompleted() {
        enqueueEvent("verification_completed", null, null, null, null);
    }

    public void trackComplaintCreationStarted() {
        enqueueEvent("complaint_creation_started", null, null, null, null);
    }

    public void trackComplaintDraftSaved() {
        enqueueEvent("complaint_draft_saved", null, null, null, null);
    }

    public void trackComplaintSubmissionAttempted(String complaintId) {
        enqueueEvent("complaint_submission_attempted", null, null, complaintId, null);
    }

    public void trackComplaintSubmissionFailed(String complaintId, String failureClass) {
        String boundedClass = sanitizeFailureClass(failureClass);
        Map<String, Object> props = new HashMap<>();
        props.put("failureClass", boundedClass);
        enqueueEvent("complaint_submission_failed", props, null, complaintId, null);
    }

    public void trackMediaAttachmentStarted(String mediaType) {
        Map<String, Object> props = new HashMap<>();
        props.put("mediaType", sanitizeMediaType(mediaType));
        enqueueEvent("media_attachment_started", props, null, null, null);
    }

    public void trackMediaAttachmentFailed(String mediaType, String failureClass) {
        Map<String, Object> props = new HashMap<>();
        props.put("mediaType", sanitizeMediaType(mediaType));
        props.put("failureClass", sanitizeMediaFailureClass(failureClass));
        enqueueEvent("media_attachment_failed", props, null, null, null);
    }

    public void trackComplaintViewed(String complaintId, String viewContext) {
        Map<String, Object> props = new HashMap<>();
        props.put("viewContext", sanitizeViewContext(viewContext));
        enqueueEvent("complaint_viewed", props, null, complaintId, null);
    }

    public void trackComplaintSupported(String complaintId) {
        Map<String, Object> props = new HashMap<>();
        props.put("supportState", "SUPPORTED");
        enqueueEvent("complaint_supported", props, null, complaintId, null);
    }

    public void trackNotificationOpened(String channel, String notificationType, String actionType) {
        Map<String, Object> props = new HashMap<>();
        props.put("channel", sanitizeChannel(channel));
        if (notificationType != null) props.put("notificationType", notificationType);
        if (actionType != null) props.put("actionType", actionType);
        enqueueEvent("notification_opened", props, null, null, null);
    }

    public void trackNotificationActionCompleted(String channel, String notificationType, String actionType) {
        Map<String, Object> props = new HashMap<>();
        props.put("channel", sanitizeChannel(channel));
        if (notificationType != null) props.put("notificationType", notificationType);
        if (actionType != null) props.put("actionType", actionType);
        enqueueEvent("notification_action_completed", props, null, null, null);
    }

    public void trackResolutionFeedbackSubmitted(String feedbackType) {
        Map<String, Object> props = new HashMap<>();
        if (feedbackType != null) props.put("feedbackType", feedbackType);
        enqueueEvent("resolution_feedback_submitted", props, null, null, null);
    }

    public void enqueueEvent(String eventName, Map<String, Object> properties, String locationId, String complaintId, String requestId) {
        if (eventName == null || !ALLOWED_EVENTS.contains(eventName)) {
            Log.w(TAG, "Attempted to log forbidden or invalid analytics event name: " + eventName);
            return;
        }

        executorService.execute(() -> {
            try {
                int currentQueueSize = analyticsEventDao.getQueueSize();
                if (currentQueueSize >= MAX_QUEUE_SIZE) {
                    analyticsEventDao.deleteOldestEvents(DELETE_BATCH_SIZE);
                }

                String idempotencyKey = UUID.randomUUID().toString();
                String occurredAt = formatIso8601Utc(new Date());
                String anonymousUserId = sessionManager.getAnonymousUserId();
                String sessionId = sessionManager.getSessionId();
                String appVersion = getAppVersion();

                AnalyticsEvent event = new AnalyticsEvent(
                        idempotencyKey,
                        1,
                        eventName,
                        occurredAt,
                        anonymousUserId,
                        sessionId,
                        "ANDROID",
                        appVersion,
                        locationId,
                        complaintId,
                        requestId,
                        properties != null ? properties : new HashMap<>()
                );

                String payloadJson = gson.toJson(event);
                AnalyticsEventEntity entity = new AnalyticsEventEntity(
                        idempotencyKey,
                        eventName,
                        payloadJson,
                        System.currentTimeMillis(),
                        0,
                        0,
                        null
                );

                analyticsEventDao.insert(entity);
                scheduleSyncWorker();
            } catch (Throwable t) {
                // Best-effort guarantee: Analytics must never break or crash app flows
                Log.e(TAG, "Failed to enqueue analytics event", t);
            }
        });
    }

    public void clearAnalyticsQueue() {
        executorService.execute(() -> {
            try {
                analyticsEventDao.clearAll();
            } catch (Throwable t) {
                Log.e(TAG, "Failed to clear analytics queue", t);
            }
        });
    }

    private void scheduleSyncWorker() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(AnalyticsSyncWorker.class)
                    .setConstraints(constraints)
                    .addTag("analytics_sync")
                    .build();

            workManager.enqueueUniqueWork(
                    "analytics_sync_work",
                    ExistingWorkPolicy.KEEP,
                    syncRequest
            );
        } catch (Throwable t) {
            Log.e(TAG, "Failed to schedule AnalyticsSyncWorker", t);
        }
    }

    public static String formatIso8601Utc(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date != null ? date : new Date());
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private String getConnectivityStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        return "ONLINE";
                    }
                }
                return "OFFLINE";
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }

    private String sanitizeFailureClass(String input) {
        if (input == null) return "UNKNOWN";
        String upper = input.toUpperCase(Locale.US);
        if (Arrays.asList("NETWORK", "TIMEOUT", "VALIDATION", "AUTHENTICATION", "SERVER").contains(upper)) {
            return upper;
        }
        return "UNKNOWN";
    }

    private String sanitizeMediaType(String input) {
        if (input == null) return "PHOTO";
        String upper = input.toUpperCase(Locale.US);
        if ("VOICE_NOTE".equals(upper) || "AUDIO".equals(upper)) return "VOICE_NOTE";
        return "PHOTO";
    }

    private String sanitizeMediaFailureClass(String input) {
        if (input == null) return "UNKNOWN";
        String upper = input.toUpperCase(Locale.US);
        if (Arrays.asList("PERMISSION", "SIZE", "FORMAT", "STORAGE", "CANCELLED").contains(upper)) {
            return upper;
        }
        return "UNKNOWN";
    }

    private String sanitizeViewContext(String input) {
        if (input == null) return "OTHER";
        String upper = input.toUpperCase(Locale.US);
        if (Arrays.asList("FEED", "OWN_COMPLAINT", "NOTIFICATION", "DEEP_LINK").contains(upper)) {
            return upper;
        }
        return "OTHER";
    }

    private String sanitizeChannel(String input) {
        if (input == null) return "IN_APP";
        String upper = input.toUpperCase(Locale.US);
        if (Arrays.asList("PUSH", "IN_APP", "EMAIL").contains(upper)) {
            return upper;
        }
        return "IN_APP";
    }
}
