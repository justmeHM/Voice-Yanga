package com.voiceyanga.citizen.data.remote;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import java.util.List;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

@HiltWorker
public class SyncWorker extends Worker {

    private final ComplaintDao complaintDao;

    @AssistedInject
    public SyncWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters params,
            ComplaintDao complaintDao) {
        super(context, params);
        this.complaintDao = complaintDao;
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

                // Simulate network latency for each complaint
                Thread.sleep(2000);

                // Mock server response (Success)
                complaint.setSyncStatus("SYNCED");
                complaint.setServerId("SRV-" + System.currentTimeMillis());
                complaint.setReferenceCode("VY-" + (100000 + (int)(Math.random() * 900000)));
                
                complaintDao.update(complaint);
            } catch (InterruptedException e) {
                return Result.retry();
            } catch (Exception e) {
                complaint.setSyncStatus("FAILED");
                complaintDao.update(complaint);
                return Result.failure();
            }
        }

        return Result.success();
    }
}