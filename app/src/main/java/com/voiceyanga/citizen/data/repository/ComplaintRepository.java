package com.voiceyanga.citizen.data.repository;

import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.remote.SyncWorker;
import com.voiceyanga.citizen.data.remote.api.ApiService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

@Singleton
public class ComplaintRepository {

    private final ComplaintDao complaintDao;
    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final ExecutorService executorService;
    private final WorkManager workManager;

    @Inject
    public ComplaintRepository(ComplaintDao complaintDao, ApiService apiService, SessionManager sessionManager, WorkManager workManager) {
        this.complaintDao = complaintDao;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.executorService = Executors.newSingleThreadExecutor();
        this.workManager = workManager;
    }

    public LiveData<List<Complaint>> getAllComplaints() {
        refreshComplaints();
        return complaintDao.getAllComplaints();
    }

    private void refreshComplaints() {
        executorService.execute(() -> {
            try {
                Response<List<Complaint>> response = apiService.getComplaints().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<Complaint> serverComplaints = response.body();
                    for (Complaint serverComplaint : serverComplaints) {
                        // Mark as synced since it came from server
                        serverComplaint.setSyncStatus("SYNCED");
                        complaintDao.insert(serverComplaint);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Refresh failed", e);
            }
        });
    }

    public LiveData<List<Complaint>> getMyComplaints(String email) {
        return complaintDao.getMyComplaints(email);
    }

    public LiveData<List<Complaint>> getCommunityComplaints(String email) {
        return complaintDao.getCommunityComplaints(email);
    }

    public LiveData<Complaint> getComplaint(String uuid) {
        return complaintDao.getComplaintByUuidLiveData(uuid);
    }

    public LiveData<List<Comment>> getComments(String complaintUuid) {
        return complaintDao.getCommentsForComplaint(complaintUuid);
    }

    public LiveData<List<ComplaintPhoto>> getPhotos(String complaintUuid) {
        return complaintDao.getPhotosForComplaint(complaintUuid);
    }

    /**
     * Simulates backend progress by updating a complaint's status and adding mock comments.
     * [Rule 73] This supports the "Mobile First, API Second" strategy with realistic mock data.
     */
    public void simulateProgress(String uuid) {
        executorService.execute(() -> {
            Complaint complaint = complaintDao.getComplaintByUuid(uuid);
            if (complaint == null || !"SYNCED".equals(complaint.getSyncStatus())) return;

            String currentStatus = complaint.getStatus();
            String nextStatus;
            String commentMsg;

            switch (currentStatus) {
                case "SUBMITTED":
                    nextStatus = "REVIEWED";
                    commentMsg = "Your report has been reviewed by our triage team.";
                    break;
                case "REVIEWED":
                    nextStatus = "ASSIGNED";
                    commentMsg = "A technician from the Matero Water and Sewerage department has been assigned.";
                    break;
                case "ASSIGNED":
                    nextStatus = "IN_PROGRESS";
                    commentMsg = "The technician is on-site investigating the issue.";
                    break;
                case "IN_PROGRESS":
                    nextStatus = "RESOLVED";
                    commentMsg = "The issue has been fixed and verified. Thank you for reporting!";
                    break;
                default:
                    return;
            }

            complaint.setStatus(nextStatus);
            complaint.setUpdatedAt(System.currentTimeMillis());
            complaintDao.update(complaint);

            Comment comment = new Comment(
                    UUID.randomUUID().toString(),
                    uuid,
                    "Official Admin",
                    commentMsg,
                    true,
                    System.currentTimeMillis()
            );
            complaintDao.insertComment(comment);
        });
    }

    public void supportComplaint(String uuid) {
        executorService.execute(() -> {
            complaintDao.incrementSupportCount(uuid);
            // In a real app, we would also sync this to the server
        });
    }

    public void saveComplaint(Complaint complaint, List<String> photoUris) {
        executorService.execute(() -> {
            complaint.setAuthorEmail(sessionManager.getUserEmail());
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

    /**
     * Schedules a background synchronization task with exponential backoff.
     * [Rule 26] Implements exponential backoff policy for retries.
     */
    public void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS)
                .addTag("complaint_sync")
                .build();

        // Use UNIQUE work to avoid overlapping sync sessions
        workManager.enqueueUniqueWork(
                "complaint_sync_unique",
                ExistingWorkPolicy.KEEP,
                syncRequest
        );
    }

    public List<Complaint> getPendingComplaints() {
        return complaintDao.getPendingComplaints();
    }
}