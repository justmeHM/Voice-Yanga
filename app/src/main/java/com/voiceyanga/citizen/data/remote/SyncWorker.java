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

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            ComplaintDao complaintDao,
            NotificationHelper notificationHelper,
            ApiService apiService) {
        super(context, params);
        this.complaintDao = complaintDao;
        this.notificationHelper = notificationHelper;
        this.apiService = apiService;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync iteration. Attempt: " + getRunAttemptCount());
        
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

        // 1. UPLOAD PHOTOS FIRST [FR-COMP-03]
        List<ComplaintPhoto> localPhotos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
        java.util.List<String> uploadedUrls = new java.util.ArrayList<>();
        
        if (!localPhotos.isEmpty()) {
            for (ComplaintPhoto localPhoto : localPhotos) {
                Log.d(TAG, "Uploading photo with label: " + localPhoto.getLabel());
                MultipartBody.Part filePart = prepareFilePart(localPhoto.getPhotoUri());
                if (filePart != null) {
                    Response<PhotoUploadResponse> photoResponse = apiService.uploadPhoto(filePart).execute();
                    if (photoResponse.isSuccessful() && photoResponse.body() != null) {
                        uploadedUrls.add(photoResponse.body().getPhotoUrl());
                    } else {
                        throw new Exception("Photo upload failed: " + photoResponse.code());
                    }
                }
            }
        }

        // 2. SUBMIT COMPLAINT DATA AS JSON
        ComplaintRequest request = new ComplaintRequest(
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getCategory(),
                complaint.getLocation()
        );

        Response<ComplaintResponse> response = apiService.createComplaint(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            ComplaintResponse result = response.body();
            
            // 3. RECONCILE SERVER ID [Rule 23]
            complaint.setSyncStatus("SYNCED");
            complaint.setServerId(result.getServerId());
            complaint.setReferenceCode(result.getReferenceCode());
            
            complaintDao.update(complaint);
            Log.d(TAG, "Sync successful for complaint: " + complaint.getReferenceCode());

            // Notify user of success
            notificationHelper.showNotification(
                    getApplicationContext().getString(R.string.sync_notification_title),
                    getApplicationContext().getString(R.string.sync_notification_message, complaint.getReferenceCode()),
                    complaint.getClientUuid(),
                    "STATUS_CHANGE"
            );
        } else {
            String errorBody = "";
            try (okhttp3.ResponseBody body = response.errorBody()) {
                if (body != null) {
                    errorBody = body.string();
                }
            } catch (Exception ignored) {}
            Log.e(TAG, "Complaint submission failed (" + response.code() + "): " + errorBody);
            throw new Exception("Complaint submission failed (" + response.code() + "): " + errorBody);
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

    private MultipartBody.Part prepareFilePart(String uriString) {
        try {
            // 1. SMART COMPRESSION [UX-06]
            File compressedFile = com.voiceyanga.citizen.core.utils.ImageCompressor.compress(getApplicationContext(), uriString);
            
            if (compressedFile == null) {
                // Fallback to original logic if compression fails
                android.net.Uri uri = android.net.Uri.parse(uriString);
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                if (inputStream == null) return null;

                compressedFile = new File(getApplicationContext().getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream out = new FileOutputStream(compressedFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                inputStream.close();
            }

            RequestBody requestFile = RequestBody.create(compressedFile, MediaType.parse("image/jpeg"));
            return MultipartBody.Part.createFormData("file", compressedFile.getName(), requestFile);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing file part", e);
            return null;
        }
    }
}
