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
import com.voiceyanga.citizen.data.local.entity.PendingAction;
import com.voiceyanga.citizen.data.remote.SyncWorker;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintDto;
import com.voiceyanga.citizen.data.remote.dto.PaginatedResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        refreshComplaints(Collections.emptyMap());
        return complaintDao.getAllComplaints();
    }

    public LiveData<List<Complaint>> getAllComplaints(Map<String, String> filters) {
        refreshComplaints(filters);
        return complaintDao.getAllComplaints();
    }

    private void refreshComplaints(Map<String, String> filters) {
        executorService.execute(() -> {
            try {
                Response<PaginatedResponse<ComplaintDto>> response = apiService.getComplaints(filters).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<ComplaintDto> serverComplaints = response.body().getData();
                    if (serverComplaints != null) {
                        for (ComplaintDto dto : serverComplaints) {
                            String targetUuid = dto.getClientUuid();
                            
                            // Reconciliation: If server didn't return clientUuid, try to find the local record by title/author
                            if (targetUuid == null || targetUuid.isEmpty()) {
                                Complaint localMatch = complaintDao.findLocalPendingMatch(dto.getTitle(), dto.getAuthorEmail());
                                if (localMatch != null) {
                                    targetUuid = localMatch.getClientUuid();
                                } else {
                                    targetUuid = dto.getServerId();
                                }
                            }

                            // First, try to find if we have local photos for this server ID
                            String localPreview = complaintDao.getFirstPhotoUri(targetUuid);

                            Complaint serverComplaint = new Complaint(
                                    targetUuid,
                                    dto.getTitle(),
                                    dto.getDescription(),
                                    dto.getCategory() != null ? dto.getCategory().getName() : "General",
                                    dto.getLocation() != null ? dto.getLocation().getDisplayName() : "Lusaka",
                                    "SYNCED",
                                    dto.getCreatedAt()
                            );
                            
                            // POPULATE PHOTO PREVIEW
                            if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
                                serverComplaint.setFirstPhotoUri(dto.getPhotos().get(0));
                            } else {
                                serverComplaint.setFirstPhotoUri(localPreview);
                            }
                            
                            if (dto.getLocation() != null) {
                                serverComplaint.setWard(dto.getLocation().getWard());
                                serverComplaint.setDistrict(dto.getLocation().getDistrict());
                                serverComplaint.setProvince(dto.getLocation().getProvince());
                                serverComplaint.setLocationId(dto.getLocation().getId());
                            }

                            // PRESERVE LOCAL-ONLY FLAGS
                            Complaint existing = complaintDao.getComplaintByUuid(serverComplaint.getClientUuid());
                            if (existing != null) {
                                serverComplaint.setSupportedByMe(existing.isSupportedByMe());
                                serverComplaint.setCommentCount(existing.getCommentCount());
                                
                                // FAILSAFE: If server has no photos, keep local cached photo
                                if (serverComplaint.getFirstPhotoUri() == null || serverComplaint.getFirstPhotoUri().isEmpty()) {
                                    serverComplaint.setFirstPhotoUri(existing.getFirstPhotoUri());
                                }
                            }
                            
                            complaintDao.insert(serverComplaint);
                        }
                    }
                } else {
                    String errorBody = "";
                    try (okhttp3.ResponseBody body = response.errorBody()) {
                        if (body != null) {
                            errorBody = body.string();
                        }
                    } catch (Exception ignored) {}
                    android.util.Log.e("ComplaintRepo", "Refresh failed (" + response.code() + "): " + errorBody);
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Refresh failed", e);
            }
        });
    }

    public void refreshComments(String serverId, String complaintUuid) {
        executorService.execute(() -> {
            try {
                Response<BaseResponse<List<CommentResponse>>> response = apiService.getComments(serverId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    for (CommentResponse dto : response.body().getData()) {
                        // RECONCILIATION: Delete local duplicate if it exists
                        complaintDao.deleteLocalComment(complaintUuid, dto.getAuthorName(), dto.getMessage());
                        
                        Comment comment = new Comment(
                                dto.getId(),
                                complaintUuid,
                                dto.getAuthorName(),
                                dto.getMessage(),
                                dto.isOfficial(),
                                System.currentTimeMillis() // Simplification: API uses ISO strings
                        );
                        complaintDao.insertComment(comment);
                    }
                    complaintDao.updateCommentCount(complaintUuid);
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Comment refresh failed", e);
            }
        });
    }

    public void postComment(String complaintUuid, String serverId, String message, boolean isCitizenVisible) {
        executorService.execute(() -> {
            String author = sessionManager.getUserName();
            if (author == null || author.isEmpty()) author = "Anonymous User";
            
            // Save locally first for immediate feedback
            Comment localComment = new Comment(
                    UUID.randomUUID().toString(),
                    complaintUuid,
                    author,
                    message,
                    false,
                    System.currentTimeMillis()
            );
            complaintDao.insertComment(localComment);
            complaintDao.updateCommentCount(complaintUuid);

            // Queue for background sync
            complaintDao.insertPendingAction(new PendingAction("COMMENT", complaintUuid, message));
            scheduleSync();
        });
    }

    public LiveData<List<Complaint>> getMyActiveComplaints(String email) {
        return complaintDao.getMyActiveComplaints(email);
    }

    public LiveData<List<Complaint>> getMyResolvedComplaints(String email) {
        return complaintDao.getMyResolvedComplaints(email);
    }

    public LiveData<List<Complaint>> getMySupportedComplaints(String email) {
        return complaintDao.getMySupportedComplaints(email);
    }

    public LiveData<Complaint> getLatestMyComplaint(String email) {
        return complaintDao.getLatestMyComplaint(email);
    }

    public LiveData<Integer> getMyReportsCount(String email) {
        return complaintDao.getMyReportsCount(email);
    }

    public LiveData<Integer> getSupportedCount() {
        return complaintDao.getSupportedCount();
    }

    public LiveData<List<Complaint>> getOutboxComplaints() {
        return complaintDao.getOutboxComplaints();
    }

    public LiveData<List<Complaint>> getCommunityComplaints(String email) {
        return getCommunityComplaints(null, null, null, null, null, null);
    }

    public LiveData<List<Complaint>> getCommunityComplaints(String status, String category, String query, String ward, String district, String province) {
        // Refresh from server too
        Map<String, String> filters = new HashMap<>();
        if (status != null) filters.put("status", status);
        if (category != null) filters.put("category", category);
        if (query != null && !query.isEmpty()) filters.put("search", query);
        if (ward != null) filters.put("ward", ward);
        if (district != null) filters.put("district", district);
        if (province != null) filters.put("province", province);
        refreshComplaints(filters);

        return complaintDao.getFilteredCommunityComplaints(status, category, query, ward, district, province);
    }

    public LiveData<Complaint> getComplaint(String uuid) {
        return complaintDao.getComplaintByUuidLiveData(uuid);
    }

    public Complaint getComplaintSync(String uuid) {
        return complaintDao.getComplaintByUuid(uuid);
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
        // Disabled mocking [Rule 73]
    }

    public void supportComplaint(String uuid) {
        executorService.execute(() -> {
            complaintDao.incrementSupportCount(uuid);
            
            // Queue for background sync
            complaintDao.insertPendingAction(new PendingAction("SUPPORT", uuid, null));
            scheduleSync();
        });
    }

    public void saveComplaint(Complaint complaint, List<String> photoUris, Map<String, String> photoLabels) {
        executorService.execute(() -> {
            complaint.setAuthorEmail(sessionManager.getUserEmail());
            
            if (photoUris != null && !photoUris.isEmpty()) {
                complaint.setFirstPhotoUri(photoUris.get(0));
            }
            
            complaintDao.insert(complaint);
            
            if (photoUris != null) {
                for (String uri : photoUris) {
                    ComplaintPhoto photo = new ComplaintPhoto(complaint.getClientUuid(), uri);
                    if (photoLabels != null) {
                        photo.setLabel(photoLabels.getOrDefault(uri, "General"));
                    }
                    complaintDao.insertPhoto(photo);
                }
            }
            
            scheduleSync();
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

    public void saveAsDraft(Complaint complaint) {
        executorService.execute(() -> {
            complaintDao.deleteDraft();
            complaint.setSyncStatus("DRAFT");
            complaintDao.insert(complaint);
        });
    }

    public Complaint getDraftSync() {
        return complaintDao.getDraft();
    }

    public void retryComplaint(String uuid) {
        executorService.execute(() -> {
            Complaint complaint = complaintDao.getComplaintByUuid(uuid);
            if (complaint != null) {
                complaint.setSyncStatus("PENDING");
                complaintDao.update(complaint);
                scheduleSync();
            }
        });
    }

    public void deleteDraft() {
        executorService.execute(complaintDao::deleteDraft);
    }
}