package com.voiceyanga.citizen.data.remote;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.notifications.NotificationHelper;
import com.voiceyanga.citizen.core.network.ErrorParser;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.local.entity.PendingAction;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.ComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.PhotoUploadResponse;
import com.voiceyanga.citizen.data.remote.dto.VoiceNoteUploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Worker for synchronizing complaints to the backend.
 */
@HiltWorker
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final ComplaintDao complaintDao;
    private final NotificationHelper notificationHelper;
    private final ApiService apiService;
    private final com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            ComplaintDao complaintDao,
            NotificationHelper notificationHelper,
            ApiService apiService,
            com.voiceyanga.citizen.data.local.SessionManager sessionManager) {
        super(context, params);
        this.complaintDao = complaintDao;
        this.notificationHelper = notificationHelper;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync iteration. Attempt: " + getRunAttemptCount());
        
        // 0. CHECK AUTHENTICATION
        if (sessionManager.getAccessToken() == null) {
            Log.e(TAG, "Sync aborted: No active session.");
            return Result.failure();
        }
        
        boolean hasErrors = false;

        // 1. PROCESS PENDING COMPLAINTS
        List<Complaint> pendingComplaints = complaintDao.getPendingComplaints();
        for (Complaint complaint : pendingComplaints) {
            try {
                syncComplaint(complaint);
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for complaint: " + complaint.getClientUuid(), e);
                complaint.setSyncStatus("FAILED");
                complaintDao.update(complaint);
                
                // Notify user of failure
                notificationHelper.showNotification(
                        "Sync Failed",
                        "Failed to upload report: " + complaint.getTitle() + ". Tap to retry.",
                        complaint.getClientUuid(),
                        "SYSTEM"
                );
                hasErrors = true;
            }
        }

        // 2. PROCESS PENDING ACTIONS (SUPPORT, COMMENT)
        List<PendingAction> pendingActions = complaintDao.getAllPendingActions();
        for (PendingAction action : pendingActions) {
            try {
                syncAction(action);
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for action: " + action.getId(), e);
                hasErrors = true;
            }
        }

        if (hasErrors && getRunAttemptCount() < 3) {
            return Result.retry();
        }

        return hasErrors ? Result.failure() : Result.success();
    }

    private void syncComplaint(Complaint complaint) throws Exception {
        // Update status to syncing
        complaint.setSyncStatus("SYNCING");
        complaintDao.update(complaint);

        // 1. UPLOAD VOICE NOTE IF EXISTS
        String voiceNoteUrl = complaint.getVoiceNoteUrl();
        if (voiceNoteUrl == null && complaint.getVoiceNoteLocalPath() != null) {
            File voiceFile = new File(complaint.getVoiceNoteLocalPath());
            if (voiceFile.exists()) {
                RequestBody requestFile = RequestBody.create(voiceFile, MediaType.parse("audio/mp4"));
                MultipartBody.Part body = MultipartBody.Part.createFormData("file", voiceFile.getName(), requestFile);
                RequestBody duration = RequestBody.create(String.valueOf(complaint.getVoiceNoteDuration()), MediaType.parse("text/plain"));
                
                Response<VoiceNoteUploadResponse> voiceResponse = apiService.uploadVoiceNote(body, duration).execute();
                if (voiceResponse.isSuccessful() && voiceResponse.body() != null && voiceResponse.body().isSuccess()) {
                    voiceNoteUrl = voiceResponse.body().getData().getFileUrl();
                    complaint.setVoiceNoteUrl(voiceNoteUrl);
                    complaintDao.update(complaint);
                } else {
                    logFailure("Voice note upload", voiceResponse);
                    throw new Exception("Voice note upload failed: " + voiceResponse.code());
                }
            }
        }

        // 2. PREPARE MULTIPART REQUEST FOR COMPLAINT AND PHOTOS
        List<ComplaintPhoto> localPhotos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
        java.util.List<MultipartBody.Part> photoParts = new java.util.ArrayList<>();
        
        if (!localPhotos.isEmpty()) {
            for (ComplaintPhoto localPhoto : localPhotos) {
                MultipartBody.Part photoPart = prepareImagePart(localPhoto.getPhotoUri());
                if (photoPart != null) {
                    photoParts.add(photoPart);
                }
            }
        }

        // Validate mandatory fields locally as per backend requirements
        if (complaint.getTitle().trim().length() < 5) {
            throw new Exception("Title too short (min 5 chars)");
        }

        Log.d(TAG, "Submitting complaint: " + complaint.getTitle() + " with " + photoParts.size() + " photos");

        Response<ComplaintResponse> response = apiService.createComplaint(
                toRequestBody(complaint.getTitle()),
                toRequestBody(complaint.getDescription()),
                toRequestBody(complaint.getCategory()),
                toRequestBody(complaint.getLocation()),
                toRequestBody(complaint.getPriority()),
                toRequestBody(complaint.getClientUuid()),
                toRequestBody(voiceNoteUrl),
                toRequestBody(complaint.getVoiceNoteDuration() != 0 ? String.valueOf(complaint.getVoiceNoteDuration()) : null),
                photoParts
        ).execute();

        if (response.isSuccessful() && response.body() != null) {
            ComplaintResponse result = response.body();
            complaint.setSyncStatus("SYNCED");
            complaint.setSyncProgress(null);
            complaint.setServerId(result.getServerId());
            complaint.setReferenceCode(result.getReferenceCode());
            complaintDao.update(complaint);
            
            notificationHelper.showNotification(
                    getApplicationContext().getString(R.string.sync_notification_title),
                    getApplicationContext().getString(R.string.sync_notification_message, complaint.getReferenceCode()),
                    complaint.getClientUuid(),
                    "STATUS_CHANGE"
            );
        } else {
            logFailure("Complaint submission", response);
            throw new Exception("Complaint submission failed: " + response.code());
        }
    }

    private RequestBody toRequestBody(String value) {
        if (value == null) return null;
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private void logFailure(String action, Response<?> response) {
        String url = response.raw().request().url().toString();
        String errorBody = "";
        try (ResponseBody body = response.errorBody()) {
            if (body != null) {
                errorBody = body.string();
            }
        } catch (Exception ignored) {}
        
        Log.e(TAG, action + " failed (HTTP " + response.code() + ")");
        Log.e(TAG, "Request URL: " + url);
        Log.e(TAG, "Error Body: " + errorBody);
        
        // Log field names for multipart requests
        okhttp3.RequestBody requestBody = response.raw().request().body();
        if (requestBody instanceof okhttp3.MultipartBody) {
            okhttp3.MultipartBody multipartBody = (okhttp3.MultipartBody) requestBody;
            Log.d(TAG, "Submitted fields: ");
            for (okhttp3.MultipartBody.Part part : multipartBody.parts()) {
                // Log headers to see field names and filenames, but NOT content
                Log.d(TAG, " - Part headers: " + part.headers());
            }
        }
    }

    private void syncAction(PendingAction action) throws Exception {
        Complaint complaint = complaintDao.getComplaintByUuid(action.getComplaintUuid());
        if (complaint == null || complaint.getServerId() == null) {
            // Wait for complaint to be synced first
            return;
        }

        if ("SUPPORT".equals(action.getActionType())) {
            Response<Void> response = apiService.supportComplaint(complaint.getServerId()).execute();
            if (response.isSuccessful()) {
                complaintDao.deletePendingAction(action.getId());
            } else if (response.code() == 409) {
                // Already supported, delete action
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Support failed: " + response.code());
            }
        } else if ("COMMENT".equals(action.getActionType())) {
            CommentRequest request = new CommentRequest(action.getData(), true);
            Response<CommentResponse> response = apiService.postComment(complaint.getServerId(), request).execute();
            if (response.isSuccessful() && response.body() != null) {
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Comment failed: " + response.code());
            }
        }
    }

    private MultipartBody.Part prepareImagePart(String uriString) {
        try {
            Context context = getApplicationContext();
            File compressedFile = com.voiceyanga.citizen.core.utils.ImageCompressor.compress(context, uriString);
            if (compressedFile == null) return null;

            if (compressedFile.length() > 5 * 1024 * 1024) {
                Log.e(TAG, "File too large: " + compressedFile.length());
                return null;
            }

            android.net.Uri uri = android.net.Uri.parse(uriString);
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(compressedFile, MediaType.parse(mimeType));
            return MultipartBody.Part.createFormData("photos", compressedFile.getName(), requestFile);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image part", e);
            return null;
        }
    }
}
