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
import com.voiceyanga.citizen.data.remote.dto.ComplaintDto;
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
    private final com.voiceyanga.citizen.data.repository.AnalyticsRepository analyticsRepository;

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            ComplaintDao complaintDao,
            NotificationHelper notificationHelper,
            ApiService apiService,
            com.voiceyanga.citizen.data.local.SessionManager sessionManager,
            com.voiceyanga.citizen.data.repository.AnalyticsRepository analyticsRepository) {
        super(context, params);
        this.complaintDao = complaintDao;
        this.notificationHelper = notificationHelper;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.analyticsRepository = analyticsRepository;
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
                try {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                } catch (Throwable ignored) {}
                complaint.setSyncStatus("FAILED");
                complaint.setFailureReason(com.voiceyanga.citizen.core.network.ErrorParser.parseThrowable(e));
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

        String videoUrl = complaint.getVideoUrl();
        if (videoUrl == null && complaint.getVideoLocalPath() != null) {
            File videoFile = new File(complaint.getVideoLocalPath());
            int duration = complaint.getVideoDuration();

            if (videoFile.exists() && videoFile.length() > 0 && duration <= 90) {
                videoUrl = uploadVideoFile(videoFile, duration, complaint);
                complaint.setVideoUrl(videoUrl);
                complaintDao.update(complaint);
                Log.i(TAG, "Video upload complete. fileUrl: " + videoUrl);
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
            request.videoUrl = videoUrl;
            request.videoDurationSeconds = complaint.getVideoDuration() > 0 ? complaint.getVideoDuration() : null;
            request.locationId = complaint.getLocationId();
            request.ward = complaint.getWard();
            request.district = complaint.getDistrict();
            request.province = complaint.getProvince();
            if (complaint.getLatitude() != 0.0) request.latitude = complaint.getLatitude();
            if (complaint.getLongitude() != 0.0) request.longitude = complaint.getLongitude();

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
                } else {
                    throw new Exception("Failed to prepare a photo for upload");
                }
            }

            if (photoParts.isEmpty()) {
                throw new Exception("No photos could be prepared for upload despite local photos existing.");
            }

            Log.d(TAG, "Submitting Multipart complaint: " + complaint.getClientUuid() + " with " + photoParts.size() + " photos");
            
            java.util.Map<String, RequestBody> parts = new java.util.HashMap<>();
            parts.put("title", toRequestBody(complaint.getTitle() != null ? complaint.getTitle() : "Untitled"));
            parts.put("description", toRequestBody(complaint.getDescription() != null ? complaint.getDescription() : ""));
            parts.put("category", toRequestBody(complaint.getCategory() != null ? complaint.getCategory() : "General"));
            parts.put("location", toRequestBody(complaint.getLocation() != null ? complaint.getLocation() : "Unknown"));
            parts.put("priority", toRequestBody(complaint.getPriority() != null ? complaint.getPriority() : "MEDIUM"));
            parts.put("clientUuid", toRequestBody(complaint.getClientUuid()));
            
            if (complaint.getLocationId() != null) parts.put("locationId", toRequestBody(complaint.getLocationId()));
            if (complaint.getWard() != null) parts.put("ward", toRequestBody(complaint.getWard()));
            if (complaint.getDistrict() != null) parts.put("district", toRequestBody(complaint.getDistrict()));
            if (complaint.getProvince() != null) parts.put("province", toRequestBody(complaint.getProvince()));
            if (complaint.getLatitude() != 0.0) parts.put("latitude", toRequestBody(String.valueOf(complaint.getLatitude())));
            if (complaint.getLongitude() != 0.0) parts.put("longitude", toRequestBody(String.valueOf(complaint.getLongitude())));

            if (voiceNoteUrl != null) {
                parts.put("voiceNoteUrl", toRequestBody(voiceNoteUrl));
            }
            if (complaint.getVoiceNoteDuration() != 0) {
                parts.put("voiceNoteDurationSeconds", toRequestBody(String.valueOf(complaint.getVoiceNoteDuration())));
            }
            if (videoUrl != null) {
                parts.put("videoUrl", toRequestBody(videoUrl));
            }
            if (complaint.getVideoDuration() != 0) {
                parts.put("videoDurationSeconds", toRequestBody(String.valueOf(complaint.getVideoDuration())));
            }

            response = apiService.createComplaintWithPhotos(parts, photoParts).execute();
        }

        if (response.isSuccessful() && response.body() != null) {
            CreateComplaintResponse result = response.body();
            complaint.setSyncStatus("SYNCED");
            complaint.setServerId(result.getServerId());
            complaint.setReferenceCode(result.referenceCode);
            complaintDao.update(complaint);

            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        .subscribeToTopic("complaint_" + result.getServerId());
            } catch (Exception ignored) {}
            
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
                        if (dto != null && dto.getClientUuid() != null && local.getClientUuid().equals(dto.getClientUuid())) {
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
            if (response != null && response.errorBody() != null) {
                errorBody = response.errorBody().string();
            }
        } catch (IOException ignored) {}

        if (analyticsRepository != null) {
            String failureClass = "SERVER";
            if (response != null) {
                int code = response.code();
                if (code == 401 || code == 403) failureClass = "AUTHENTICATION";
                else if (code == 400 || code == 422) failureClass = "VALIDATION";
                else if (code == 408 || code == 504) failureClass = "TIMEOUT";
                else if (code >= 500) failureClass = "SERVER";
            }
            analyticsRepository.trackComplaintSubmissionFailed(complaint.getClientUuid(), failureClass);
        }

        Log.e(TAG, String.format(
            "[DIAGNOSTICS] Action: %s | Status: %d | isMultipart: %b | clientUuid: %s | voiceNote: %b | photos: %d | Body: %s",
            action, response != null ? response.code() : 0, isMultipart, complaint.getClientUuid(), 
            complaint.getVoiceNoteUrl() != null, 
            complaintDao.getPhotosForComplaintSync(complaint.getClientUuid()).size(),
            errorBody
        ));
    }

    private RequestBody toRequestBody(String value) {
        if (value == null) return null;
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private void syncAction(PendingAction action) throws Exception {
        Complaint complaint = complaintDao.getComplaintByUuid(action.getComplaintUuid());
        if (complaint == null || complaint.getServerId() == null) {
            return;
        }

        if ("SUPPORT".equals(action.getActionType())) {
            Response<com.voiceyanga.citizen.data.remote.dto.SupportResponse> response = apiService.support(complaint.getServerId()).execute();
            if (response.isSuccessful() || response.code() == 409) {
                complaintDao.deletePendingAction(action.getId());
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                            .subscribeToTopic("complaint_" + complaint.getServerId());
                } catch (Exception ignored) {}
            } else {
                throw new Exception("Support failed");
            }
        } else if ("COMMENT".equals(action.getActionType())) {
            CommentRequest request = new CommentRequest(action.getData(), true);
            Response<ApiEnvelope<CommentResponse>> response = apiService.addComment(complaint.getServerId(), request).execute();
            if (response.isSuccessful() && response.body() != null) {
                complaintDao.deletePendingAction(action.getId());
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                            .subscribeToTopic("complaint_" + complaint.getServerId());
                } catch (Exception ignored) {}
            } else {
                throw new Exception("Comment failed");
            }
        } else if ("EDIT_COMMENT".equals(action.getActionType())) {
            String rawData = action.getData();
            if (rawData != null && rawData.contains("|||")) {
                String[] parts = rawData.split("\\|\\|\\|", 2);
                String commentId = parts[0];
                String newContent = parts[1];
                CommentRequest request = new CommentRequest(newContent, true);
                Response<ApiEnvelope<CommentResponse>> response = apiService.updateComment(complaint.getServerId(), commentId, request).execute();
                if (response.isSuccessful() || response.code() == 404) {
                    complaintDao.deletePendingAction(action.getId());
                } else {
                    throw new Exception("Edit comment failed");
                }
            } else {
                complaintDao.deletePendingAction(action.getId());
            }
        } else if ("DELETE_COMMENT".equals(action.getActionType())) {
            String commentId = action.getData();
            Response<ApiEnvelope<Object>> response = apiService.deleteComment(complaint.getServerId(), commentId).execute();
            if (response.isSuccessful() || response.code() == 404) {
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Delete comment failed");
            }
        } else if ("EDIT_COMPLAINT".equals(action.getActionType())) {
            String jsonStr = action.getData();
            ComplaintRequest request = new com.google.gson.Gson().fromJson(jsonStr, ComplaintRequest.class);
            if (request == null) {
                request = new ComplaintRequest(complaint.getTitle(), complaint.getDescription(), complaint.getCategory(), complaint.getLocation(), complaint.getPriority(), complaint.getClientUuid());
            }
            Response<ApiEnvelope<ComplaintDto>> response = apiService.updateComplaint(complaint.getServerId(), request).execute();
            if (response.isSuccessful() || response.code() == 404) {
                complaintDao.deletePendingAction(action.getId());
            } else {
                throw new Exception("Edit complaint failed");
            }
        }
    }

    private String uploadVideoFile(File videoFile, int duration, Complaint complaint) throws Exception {
        int safeDuration = Math.max(1, Math.min(90, duration));
        String filename = videoFile.getName();
        String mimeType = "video/mp4";

        try {
            com.voiceyanga.citizen.data.remote.dto.PresignedUploadRequest presignedReq =
                    new com.voiceyanga.citizen.data.remote.dto.PresignedUploadRequest(filename, mimeType, videoFile.length());
            
            Response<ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.PresignedUploadResponse>> presignedResponse =
                    apiService.getPresignedVideoUploadUrl(presignedReq).execute();

            if (presignedResponse.isSuccessful() && presignedResponse.body() != null && presignedResponse.body().success) {
                com.voiceyanga.citizen.data.remote.dto.PresignedUploadResponse data = presignedResponse.body().data;
                if (data != null && data.uploadUrl != null && data.fileUrl != null) {
                    RequestBody fileBody = RequestBody.create(videoFile, MediaType.parse(mimeType));
                    Response<Void> putResponse = apiService.uploadBinaryToUrl(data.uploadUrl, fileBody).execute();
                    if (putResponse.isSuccessful()) {
                        Log.i(TAG, "Presigned video upload successful. fileUrl: " + data.fileUrl);
                        return data.fileUrl;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Presigned video upload attempt failed or not supported, trying legacy endpoint", e);
        }

        if (videoFile.length() > 4.2 * 1024 * 1024) {
            long sizeMb = videoFile.length() / (1024 * 1024);
            throw new Exception("Video upload failed: File size (" + sizeMb + "MB) exceeds Vercel 4.5MB serverless limit. Direct upload endpoint (POST /videos/upload-url) required.");
        }

        RequestBody videoBody = RequestBody.create(videoFile, MediaType.parse(mimeType));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", filename, videoBody);

        RequestBody durationPart = RequestBody.create(
                String.valueOf(safeDuration),
                MediaType.parse("text/plain")
        );

        Response<ApiEnvelope<VoiceNoteData>> videoResponse = apiService.uploadVideo(filePart, durationPart).execute();

        if (videoResponse.isSuccessful() && videoResponse.body() != null && videoResponse.body().success) {
            Log.i(TAG, "Legacy video upload successful. fileUrl: " + videoResponse.body().data.fileUrl);
            return videoResponse.body().data.fileUrl;
        } else {
            recordFailure("Video upload", videoResponse, complaint, false);
            String error = com.voiceyanga.citizen.core.network.ErrorParser.parseError(videoResponse);
            throw new Exception("Video upload failed: " + error);
        }
    }

    private MultipartBody.Part prepareImagePart(String uriString) {
        try {
            if (uriString == null || uriString.isEmpty()) return null;
            
            if (uriString.startsWith("/")) {
                File file = new File(uriString);
                if (!file.exists()) {
                    Log.e(TAG, "File does not exist: " + uriString);
                    return null;
                }
                RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
                return MultipartBody.Part.createFormData("photos", file.getName(), requestFile);
            } else {
                android.net.Uri uri = android.net.Uri.parse(uriString);
                File compressed = com.voiceyanga.citizen.core.utils.ImageCompressor.compress(getApplicationContext(), uriString);
                if (compressed != null && compressed.exists()) {
                    RequestBody requestFile = RequestBody.create(compressed, MediaType.parse("image/jpeg"));
                    return MultipartBody.Part.createFormData("photos", compressed.getName(), requestFile);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to prepare image part", e);
        }
        return null;
    }
}
