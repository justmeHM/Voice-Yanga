package com.voiceyanga.citizen.data.remote;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.voiceyanga.citizen.core.notifications.NotificationHelper;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import java.util.List;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import android.util.Log;

/**
 * Worker for synchronizing complaints to the backend.
 * [FR-COMP-02] Background synchronization.
 * [FR-COMP-03] Photo synchronization support.
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
        List<Complaint> pendingComplaints = complaintDao.getPendingComplaints();

        if (pendingComplaints.isEmpty()) {
            return Result.success();
        }

        for (Complaint complaint : pendingComplaints) {
            try {
                // Update status to syncing
                complaint.setSyncStatus("SYNCING");
                complaintDao.update(complaint);

                // Fetch photos for this complaint
                List<ComplaintPhoto> photos = complaintDao.getPhotosForComplaintSync(complaint.getClientUuid());
                if (!photos.isEmpty()) {
                    Log.d(TAG, "Uploading " + photos.size() + " photos for complaint: " + complaint.getClientUuid());
                    // Simulate photo upload delay
                    Thread.sleep(1000 * photos.size());
                }

                // Simulate network latency for each complaint
                Thread.sleep(1000);

                // Mock server response (Success)
                complaint.setSyncStatus("SYNCED");
                complaint.setServerId("SRV-" + System.currentTimeMillis());
                complaint.setReferenceCode("VY-" + (100000 + (int)(Math.random() * 900000)));
                
                complaintDao.update(complaint);
                Log.d(TAG, "Sync successful for complaint: " + complaint.getReferenceCode());
                
                // Mock Notification
                notificationHelper.showNotification(
                        "Report Submitted",
                        "Your report " + complaint.getReferenceCode() + " has been successfully submitted.",
                        complaint.getClientUuid(),
                        "STATUS_CHANGE"
                );
            } catch (InterruptedException e) {
                return Result.retry();
            } catch (Exception e) {
                Log.e(TAG, "Sync failed for complaint: " + complaint.getClientUuid(), e);
                complaint.setSyncStatus("FAILED");
                complaintDao.update(complaint);
                return Result.failure();
            }
        }

        return Result.success();
    }
}