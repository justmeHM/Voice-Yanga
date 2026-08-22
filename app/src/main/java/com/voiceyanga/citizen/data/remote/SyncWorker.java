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
import java.util.List;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

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

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            ComplaintDao complaintDao,
            NotificationHelper notificationHelper) {
        super(context, params);
        this.complaintDao = complaintDao;
        this.notificationHelper = notificationHelper;
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
                List<ComplaintPhoto> photos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
                if (!photos.isEmpty()) {
                    Log.d(TAG, "Uploading " + photos.size() + " photos for complaint: " + complaint.getClientUuid());
                    // Simulate photo upload delay
                    Thread.sleep(800L * photos.size());
                    
                    // Simulate random photo upload failure (10% chance) for testing retries
                    if (Math.random() < 0.1) {
                        throw new Exception("Temporary photo upload failure");
                    }
                }

                // 2. SUBMIT COMPLAINT DATA
                // Simulate network latency
                Thread.sleep(1000);

                // 3. RECONCILE SERVER ID [Rule 23]
                complaint.setSyncStatus("SYNCED");
                complaint.setServerId("SRV-" + System.currentTimeMillis());
                complaint.setReferenceCode("VY-" + (100000 + (int)(Math.random() * 900000)));
                
                complaintDao.update(complaint);
                Log.d(TAG, "Sync successful for complaint: " + complaint.getReferenceCode());
                
                // Notify user of success
                notificationHelper.showNotification(
                        getApplicationContext().getString(R.string.sync_notification_title),
                        getApplicationContext().getString(R.string.sync_notification_message, complaint.getReferenceCode()),
                        complaint.getClientUuid(),
                        "STATUS_CHANGE"
                );
            } catch (InterruptedException e) {
                // Task was cancelled by WorkManager
                return Result.retry();
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for complaint: " + complaint.getClientUuid(), e);
                complaint.setSyncStatus("FAILED");
                complaintDao.update(complaint);
                hasErrors = true;
                
                // If it's the first few attempts, we retry. Otherwise, we fail this worker run.
                if (getRunAttemptCount() < 3) {
                    return Result.retry();
                }
            }
        }

        return hasErrors ? Result.failure() : Result.success();
    }
}
