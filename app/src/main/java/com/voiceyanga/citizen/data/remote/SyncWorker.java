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
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ComplaintRequest;
import com.voiceyanga.citizen.data.remote.dto.ComplaintResponse;

import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import okhttp3.ResponseBody;
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

                // 1. SUBMIT COMPLAINT DATA AS JSON (Instructions: Fix Only the Complaint 400 Error)
                // Body must use category and location as strings. Do not send IDs or photos.
                ComplaintRequest request = new ComplaintRequest(
                        complaint.getTitle(),
                        complaint.getDescription(),
                        complaint.getCategory(),
                        complaint.getLocation()
                );

                Response<ComplaintResponse> response = apiService.createComplaint(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    ComplaintResponse result = response.body();
                    
                    // 2. RECONCILE SERVER ID [Rule 23]
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
                    try (ResponseBody responseBody = response.errorBody()) {
                        String errorBody = responseBody != null ? responseBody.string() : "Unknown error";
                        Log.e(TAG, "Sync failed. HTTP Status: " + response.code() + " | Response: " + errorBody);
                    }
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
}