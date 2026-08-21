package com.voiceyanga.citizen.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.SyncWorker;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ComplaintRepository {

    private final ComplaintDao complaintDao;
    private final ExecutorService executorService;
    private final WorkManager workManager;

    @Inject
    public ComplaintRepository(ComplaintDao complaintDao, @ApplicationContext Context context) {
        this.complaintDao = complaintDao;
        this.executorService = Executors.newSingleThreadExecutor();
        this.workManager = WorkManager.getInstance(context);
    }

    public LiveData<List<Complaint>> getAllComplaints() {
        return complaintDao.getAllComplaints();
    }

    public void saveComplaint(Complaint complaint) {
        executorService.execute(() -> {
            complaintDao.insert(complaint);
            scheduleSync();
        });
    }

    public void updateComplaint(Complaint complaint) {
        executorService.execute(() -> {
            complaintDao.update(complaint);
        });
    }

    public void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(syncRequest);
    }

    public List<Complaint> getPendingComplaints() {
        return complaintDao.getPendingComplaints();
    }
}