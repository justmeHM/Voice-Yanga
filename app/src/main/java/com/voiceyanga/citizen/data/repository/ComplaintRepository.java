package com.voiceyanga.citizen.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.remote.SyncWorker;
import java.util.List;
import java.util.UUID;
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

    public LiveData<Complaint> getComplaint(String uuid) {
        return complaintDao.getComplaintByUuidLiveData(uuid);
    }

    public LiveData<List<Comment>> getComments(String complaintUuid) {
        return complaintDao.getCommentsForComplaint(complaintUuid);
    }

    public void supportComplaint(String uuid) {
        executorService.execute(() -> {
            complaintDao.incrementSupportCount(uuid);
            // In a real app, we would also sync this to the server
        });
    }

    public void saveComplaint(Complaint complaint, List<String> photoUris) {
        executorService.execute(() -> {
            complaintDao.insert(complaint);
            
            if (photoUris != null) {
                for (String uri : photoUris) {
                    complaintDao.insertPhoto(new ComplaintPhoto(complaint.getClientUuid(), uri));
                }
            }
            
            scheduleSync();
            addMockOfficialComment(complaint.getClientUuid());
        });
    }

    private void addMockOfficialComment(String complaintUuid) {
        executorService.execute(() -> {
            Comment comment = new Comment(
                    UUID.randomUUID().toString(),
                    complaintUuid,
                    "Official Admin",
                    "We have received your report and it is currently being reviewed by the Matero District office.",
                    true,
                    System.currentTimeMillis() + 1000 // 1 second after submission
            );
            complaintDao.insertComment(comment);
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