package com.voiceyanga.citizen.data.remote;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.notifications.NotificationHelper;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.local.entity.PendingAction;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.CreateComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.VoiceNoteData;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        
        if (sessionManager.getAccessToken() == null) {
            Log.e(TAG, "Sync aborted: No active session.");
            return Result.failure();
        }
        
        boolean hasErrors = false;

        List<Complaint> pendingComplaints = complaintDao.getPendingComplaints();
        for (Complaint complaint : pendingComplaints) {
            try {
                // Reconciliation check if we failed with a 500 before
                if ("FAILED".equals(complaint.getSyncStatus()) && 
                    complaint.getFailureReason() != null && 
                    complaint.getFailureReason().contains("500")) {
                    Log.d(TAG, "Checking for existing submission on server for: " + complaint.getClientUuid());
                    if (reconcileWithServer(complaint)) {
                        continue; // Already synced
                    }
                }
                syncComplaint(complaint);
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for complaint: " + complaint.getClientUuid(), e);
                complaint.setSyncStatus("FAILED");
                complaint.setFailureReason(e.getMessage());
                complaintDao.update(complaint);
                
                notificationHelper.showNotification(
                        "Sync Failed",
                        "Failed to upload report: " + complaint.getTitle() + ". Tap to retry.",
                        complaint.getClientUuid(),
                        "SYSTEM"
                );
                hasErrors = true;
            }
        }

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
        complaint.setSyncStatus("SYNCING");
        complaintDao.update(complaint);

        String voiceNoteUrl = complaint.getVoiceNoteUrl();
        if (voiceNoteUrl == null && complaint.getVoiceNoteLocalPath() != null) {
            File voiceFile = new File(complaint.getVoiceNoteLocalPath());
            int duration = complaint.getVoiceNoteDuration();

            if (voiceFile.exists() && voiceFile.length() > 0 && duration >= 1 && duration <= 120) {
                String filename = voiceFile.getName();
                String mimeType = "audio/mp4";

                RequestBody audioBody = RequestBody.create(voiceFile, MediaType.parse(mimeType));
                MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", filename, audioBody);

                RequestBody durationPart = RequestBody.create(
                        String.valueOf(duration),
                        MediaType.parse("text/plain")
                );

                Response<ApiEnvelope<VoiceNoteData>> voiceResponse = apiService.uploadVoiceNote(filePart, durationPart).execute();

                if (voiceResponse.isSuccessful() && voiceResponse.body() != null && voiceResponse.body().success) {
                    voiceNoteUrl = voiceResponse.body().data.fileUrl;
                    complaint.setVoiceNoteUrl(voiceNoteUrl);
                    complaintDao.update(complaint);
                    Log.i(TAG, "Voice note upload successful. fileUrl: " + voiceNoteUrl);
                } else {
                    recordFailure("Voice note upload", voiceResponse, complaint, false);
                    String error = com.voiceyanga.citizen.core.network.ErrorParser.parseError(voiceResponse);
                    throw new Exception("Voice note upload failed: " + error);
                }
            }
        }

        List<ComplaintPhoto> localPhotos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
        
        Response<CreateComplaintResponse> response;
        if (localPhotos.isEmpty()) {
            ComplaintRequest request = new ComplaintRequest(
                    complaint.getTitle(),
                    complaint.getDescription(),
                    complaint.getCategory(),
                    complaint.getLocation(),
                    complaint.getPriority(),
                    complaint.getClientUuid()
            );
            request.voiceNoteUrl = voiceNoteUrl;
            request.voiceNoteDurationSeconds = complaint.getVoiceNoteDuration() > 0 ? complaint.getVoiceNoteDuration() : null;

            if (!request.isValid()) {
                throw new Exception("Invalid complaint data");
            }

            Log.d(TAG, "Submitting JSON complaint: " + complaint.getClientUuid());
            response = apiService.createComplaint(request).execute();
        } else {
            java.util.List<MultipartBody.Part> photoParts = new java.util.ArrayList<>();
            for (ComplaintPhoto localPhoto : localPhotos) {
                MultipartBody.Part photoPart = prepareImagePart(localPhoto.getPhotoUri());
                if (photoPart != null) {
                    photoParts.add(photoPart);
                }
            }

            Log.d(TAG, "Submitting Multipart complaint: " + complaint.getClientUuid() + " with " + photoParts.size() + " photos");
            response = apiService.createComplaintWithPhotos(
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
        }

        if (response.isSuccessful() && response.body() != null) {
            CreateComplaintResponse result = response.body();
            complaint.setSyncStatus("SYNCED");
            complaint.setServerId(result.getServerId());
            complaint.setReferenceCode(result.referenceCode);
            complaintDao.update(complaint);
            
            notificationHelper.showNotification(
                    getApplicationContext().getString(R.string.sync_notification_title),
                    getApplicationContext().getString(R.string.sync_notification_message, complaint.getReferenceCode()),
                    complaint.getClientUuid(),
                    "STATUS_CHANGE"
            );
        } else {
            recordFailure("Complaint submission", response, complaint, !localPhotos.isEmpty());
            String error = com.voiceyanga.citizen.core.network.ErrorParser.parseError(response);
            throw new Exception(error);
        }
    }

    private boolean reconcileWithServer(Complaint local) {
        try {
            Response<com.voiceyanga.citizen.data.remote.dto.ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.PaginatedComplaints>> response = apiService.getMyComplaints(0, 50).execute();
            if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                com.voiceyanga.citizen.data.remote.dto.PaginatedComplaints paginatedData = response.body().data;
                if (paginatedData.data != null) {
                    List<com.voiceyanga.citizen.data.remote.dto.ComplaintDto> serverList = paginatedData.data;
                    for (com.voiceyanga.citizen.data.remote.dto.ComplaintDto dto : serverList) {
                        if (local.getClientUuid().equals(dto.getClientUuid())) {
                            local.setSyncStatus("SYNCED");
                            local.setServerId(dto.getServerId());
                            local.setReferenceCode(dto.getReferenceCode());
                            complaintDao.update(local);
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Reconciliation failed", e);
        }
        return false;
    }

    private void recordFailure(String action, Response<?> response, Complaint complaint, boolean isMultipart) {
        String errorBody = "";
        try {
            if (response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (IOException ignored) {}

        Log.e(TAG, String.format(
            "[DIAGNOSTICS] Action: %s | Status: %d | isMultipart: %b | clientUuid: %s | voiceNote: %b | photos: %d | Body: %s",
            action, response.code(), isMultipart, complaint.getClientUuid(), 
            complaint.getVoiceNoteUrl() != null, 
            complaintDao.getPhotosForComplaintSync(complaint.getClientUuid()).size(),
            errorBody
        ));
    }

    private RequestBody toRequestBody(String value) {
        if (value == null) return null;
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private void logFailure(String action, Response<?> response) {
        Log.e(TAG, action + " failed (HTTP " + response.code() + ")");
    }

    private void syncAction(PendingAction action) throws Exception {
        Complaint complaint = complaintDao.getComplaintByUuid(action.getComplaintUuid());
        if (complaint == null || complaint.getServerId() == null) {
            return;
        }

        if ("SUPPORT".equals(action.getActionType())) {
            Response<com.voiceyanga.citizen.data.remote.dto.SupportResponse> response = apiService.support(complaint.getServerId()).execute();
            if (response.isSuccessful()) {
                complaintDao.deletePendingAction(action.getId());
            } else if (response.code() == 409) {
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Support failed");
            }
        } else if ("COMMENT".equals(action.getActionType())) {
            CommentRequest request = new CommentRequest(action.getData(), true);
            Response<ApiEnvelope<CommentResponse>> response = apiService.addComment(complaint.getServerId(), request).execute();
            if (response.isSuccessful() && response.body() != null) {
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Comment failed");
            }
        }
    }

    private MultipartBody.Part prepareImagePart(String uriString) {
        try {
            Context context = getApplicationContext();
            File compressedFile = com.voiceyanga.citizen.core.utils.ImageCompressor.compress(context, uriString);
            if (compressedFile == null) return null;

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
