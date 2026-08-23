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
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.ComplaintResponse;
import com.voiceyanga.citizen.data.remote.dto.PhotoUploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * Worker for synchronizing complaints to the backend.
 * [FR-COMP-02] Background synchronization.
 * [FR-COMP-03] Photo synchronization support.
 * [Rule 26] Robust retry and state management.
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
        
        List<Complaint> pendingComplaints = complaintDao.getPendingComplaints();

        if (pendingComplaints.isEmpty()) {
            return Result.success();
        }

        boolean hasErrors = false;

        for (Complaint complaint : pendingComplaints) {
            try {
                // Update status to syncing
                complaint.setSyncStatus("SYNCING");
                complaintDao.update(complaint);

                // 1. UPLOAD PHOTOS [FR-COMP-03]
                List<ComplaintPhoto> localPhotos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
                List<String> remotePhotoUrls = new ArrayList<>();
                
                for (ComplaintPhoto localPhoto : localPhotos) {
                    MultipartBody.Part part = prepareFilePart(localPhoto.getPhotoUri());
                    if (part != null) {
                        RequestBody uuidPart = RequestBody.create(complaint.getClientUuid(), MediaType.parse("text/plain"));
                        Response<PhotoUploadResponse> uploadResponse = apiService.uploadPhoto(part, uuidPart).execute();
                        if (uploadResponse.isSuccessful() && uploadResponse.body() != null) {
                            remotePhotoUrls.add(uploadResponse.body().getPhotoUrl());
                        } else {
                            throw new Exception("Photo upload failed for " + localPhoto.getPhotoUri() + ": " + uploadResponse.message());
                        }
                    }
                }

                // 2. SUBMIT COMPLAINT DATA
                ComplaintRequest request = new ComplaintRequest(
                        complaint.getClientUuid(),
                        complaint.getTitle(),
                        complaint.getDescription(),
                        complaint.getCategoryId(),
                        complaint.getLocationId(),
                        complaint.getPriority(),
                        complaint.getAuthorEmail(),
                        complaint.getCreatedAt(),
                        remotePhotoUrls
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
                    throw new Exception("Complaint submission failed: " + response.message());
                }
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for complaint: " + complaint.getClientUuid(), e);
                complaint.setSyncStatus("FAILED");
                complaintDao.update(complaint);
                hasErrors = true;
                
                if (getRunAttemptCount() < 3) {
                    return Result.retry();
                }
            }
        }

        return hasErrors ? Result.failure() : Result.success();
    }

    private MultipartBody.Part prepareFilePart(String uriString) {
        try {
            android.net.Uri uri = android.net.Uri.parse(uriString);
            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getApplicationContext().getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            inputStream.close();

            RequestBody requestFile = RequestBody.create(tempFile, MediaType.parse("image/jpeg"));
            return MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing file part", e);
            return null;
        }
    }
}
