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
import com.voiceyanga.citizen.core.notifications.NotificationHelper;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.CommentRequest;
import com.voiceyanga.citizen.data.remote.dto.CommentResponse;
import com.voiceyanga.citizen.data.remote.dto.ComplaintDto;
import com.voiceyanga.citizen.data.remote.dto.HistoryItem;
import com.voiceyanga.citizen.data.remote.dto.PaginatedComplaints;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
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
    private final NotificationHelper notificationHelper;

    private final androidx.lifecycle.MutableLiveData<com.voiceyanga.citizen.feature.home.CommunityFeedState> communityFeedState = new androidx.lifecycle.MutableLiveData<>(com.voiceyanga.citizen.feature.home.CommunityFeedState.createInitial());
    public LiveData<com.voiceyanga.citizen.feature.home.CommunityFeedState> getCommunityFeedState() { return communityFeedState; }

    private final androidx.lifecycle.MutableLiveData<com.voiceyanga.citizen.feature.home.MyComplaintsState> myComplaintsState = new androidx.lifecycle.MutableLiveData<>(com.voiceyanga.citizen.feature.home.MyComplaintsState.createInitial());
    public LiveData<com.voiceyanga.citizen.feature.home.MyComplaintsState> getMyComplaintsState() { return myComplaintsState; }

    @Inject
    public ComplaintRepository(ComplaintDao complaintDao, ApiService apiService, SessionManager sessionManager, WorkManager workManager, NotificationHelper notificationHelper) {
        this.complaintDao = complaintDao;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.workManager = workManager;
        this.notificationHelper = notificationHelper;
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Initial health and origin check
        checkHealth();
    }

    private void checkHealth() {
        android.util.Log.d("ComplaintRepo", "API_BASE_URL host=" + com.voiceyanga.citizen.BuildConfig.API_BASE_URL);
        executorService.execute(() -> {
            try {
                if (apiService != null) {
                    retrofit2.Call<com.voiceyanga.citizen.data.remote.dto.HealthResponse> call = apiService.ready();
                    if (call != null) {
                        Response<com.voiceyanga.citizen.data.remote.dto.HealthResponse> response = call.execute();
                        if (response != null && response.isSuccessful() && response.body() != null) {
                            android.util.Log.d("ComplaintRepo", "HEALTH communityReadPolicy=" + response.body().getCommunityReadPolicy());
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Health check failed", e);
            }
        });
    }

    public LiveData<List<Complaint>> getAllComplaints() {
        return complaintDao.getAllComplaints();
    }

    public String getUserId() {
        return sessionManager.getUserId();
    }

    public void refreshCommunityFeed() {
        com.voiceyanga.citizen.feature.home.CommunityFeedState current = communityFeedState.getValue();
        if (current != null && current.isRefreshing()) {
            android.util.Log.d("ComplaintRepo", "Community feed refresh already in progress, skipping");
            return;
        }

        executorService.execute(() -> {
            boolean isInitial = (current == null || current.getItems().isEmpty());
            communityFeedState.postValue(new com.voiceyanga.citizen.feature.home.CommunityFeedState(
                current != null ? current.getItems() : java.util.Collections.emptyList(),
                isInitial, true, current != null && current.hasCompletedSuccessfulRefresh(),
                false, null, 0, current != null ? current.getTotalPages() : 0, current != null ? current.getTotalItems() : 0
            ));

            try {
                int currentPage = 0;
                int totalPages = 1;
                int totalItems = 0;
                int accumulatedUpserted = 0;
                java.util.List<com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership> allMemberships = new java.util.ArrayList<>();

                while (currentPage < totalPages) {
                    android.util.Log.d("ComplaintRepo", "Refreshing community feed... page=" + currentPage);
                    Response<ApiEnvelope<PaginatedComplaints>> response = apiService.getComplaints(currentPage, 50, null, null, null, null, null, null).execute();
                    
                    if (!response.isSuccessful()) {
                        String parsedError = com.voiceyanga.citizen.core.network.ErrorParser.parseError(response);
                        throw new RuntimeException(parsedError);
                    }
                    if (response.body() == null || !response.body().success || response.body().data == null) {
                        String msg = (response.body() != null && response.body().message != null) ? response.body().message : "Invalid response from server";
                        throw new RuntimeException(msg);
                    }

                    PaginatedComplaints paginatedData = response.body().data;
                    if (paginatedData.data == null) {
                        throw new RuntimeException("COMMUNITY_HTTP null complaint list at page " + currentPage);
                    }

                    if (paginatedData.meta != null) {
                        totalPages = paginatedData.meta.totalPages;
                        totalItems = paginatedData.meta.totalItems;
                    }

                    int upserted = processServerComplaintsInternal(paginatedData.data);
                    accumulatedUpserted += upserted;

                    for (com.voiceyanga.citizen.data.remote.dto.ComplaintDto dto : paginatedData.data) {
                        if (dto != null && dto.getServerId() != null) {
                            allMemberships.add(new com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership(dto.getServerId(), "COMMUNITY", currentPage));
                        }
                    }

                    android.util.Log.d("ComplaintRepo", String.format("COMMUNITY_HTTP page=%d parsedCount=%d totalItems=%d totalPages=%d", 
                            currentPage, paginatedData.data.size(), totalItems, totalPages));

                    currentPage++;
                }

                java.util.Set<String> serverIdsReturned = new java.util.HashSet<>();
                for (com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership m : allMemberships) {
                    serverIdsReturned.add(m.getServerId());
                }
                java.util.List<Complaint> localSynced = complaintDao.getSyncedComplaintsByFeedType("COMMUNITY");
                if (localSynced != null) {
                    for (Complaint comp : localSynced) {
                        if (comp.getServerId() != null && !serverIdsReturned.contains(comp.getServerId())) {
                            complaintDao.purgeComplaintByServerId(comp.getServerId());
                        }
                    }
                }

                complaintDao.updateFeedMemberships("COMMUNITY", allMemberships);

                int observedCount = complaintDao.getCommunityCountSync();
                android.util.Log.d("ComplaintRepo", String.format("COMMUNITY_DB upserted=%d membershipCount=%d observedCount=%d", 
                        accumulatedUpserted, allMemberships.size(), observedCount));

                communityFeedState.postValue(new com.voiceyanga.citizen.feature.home.CommunityFeedState(
                    java.util.Collections.emptyList(),
                    false, false, true, false, null, currentPage - 1, totalPages, totalItems
                ));
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Community feed refresh failed", e);
                com.voiceyanga.citizen.feature.home.CommunityFeedState s = communityFeedState.getValue();
                String formattedError = com.voiceyanga.citizen.core.network.ErrorParser.parseThrowable(e);
                communityFeedState.postValue(new com.voiceyanga.citizen.feature.home.CommunityFeedState(
                    s != null ? s.getItems() : java.util.Collections.emptyList(),
                    false, false, s != null && s.hasCompletedSuccessfulRefresh(),
                    s != null && s.hasCompletedSuccessfulRefresh(), formattedError, 
                    s != null ? s.getPage() : 0, s != null ? s.getTotalPages() : 0, s != null ? s.getTotalItems() : 0
                ));
            }
        });
    }

    public LiveData<List<Complaint>> observeCommunityFeed(String status, String category, String query, String ward, String district, String province) {
        return complaintDao.getFilteredCommunityComplaints(status, category, query, ward, district, province);
    }

    public void refreshMyComplaints() {
        com.voiceyanga.citizen.feature.home.MyComplaintsState current = myComplaintsState.getValue();
        if (current != null && current.isRefreshing()) {
            android.util.Log.d("ComplaintRepo", "My complaints refresh already in progress, skipping");
            return;
        }

        executorService.execute(() -> {
            boolean isInitial = (current == null || current.getItems().isEmpty());
            myComplaintsState.postValue(new com.voiceyanga.citizen.feature.home.MyComplaintsState(
                current != null ? current.getItems() : java.util.Collections.emptyList(),
                isInitial, true, current != null && current.hasCompletedSuccessfulRefresh(),
                false, null, 0, current != null ? current.getTotalPages() : 0, current != null ? current.getTotalItems() : 0
            ));

            try {
                int currentPage = 0;
                int totalPages = 1;
                int totalItems = 0;
                int accumulatedUpserted = 0;
                java.util.List<com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership> allMemberships = new java.util.ArrayList<>();

                while (currentPage < totalPages) {
                    android.util.Log.d("ComplaintRepo", "Refreshing my complaints... page=" + currentPage);
                    Response<ApiEnvelope<PaginatedComplaints>> response = apiService.getMyComplaints(currentPage, 50).execute();
                    
                    if (!response.isSuccessful()) {
                        String parsedError = com.voiceyanga.citizen.core.network.ErrorParser.parseError(response);
                        throw new RuntimeException(parsedError);
                    }
                    if (response.body() == null || !response.body().success || response.body().data == null) {
                        String msg = (response.body() != null && response.body().message != null) ? response.body().message : "Invalid response from server";
                        throw new RuntimeException(msg);
                    }

                    PaginatedComplaints paginatedData = response.body().data;
                    if (paginatedData.data == null) {
                        throw new RuntimeException("MY_HTTP null complaint list at page " + currentPage);
                    }

                    if (paginatedData.meta != null) {
                        totalPages = paginatedData.meta.totalPages;
                        totalItems = paginatedData.meta.totalItems;
                    }

                    int upserted = processServerComplaintsInternal(paginatedData.data);
                    accumulatedUpserted += upserted;

                    for (com.voiceyanga.citizen.data.remote.dto.ComplaintDto dto : paginatedData.data) {
                        if (dto != null && dto.getServerId() != null) {
                            allMemberships.add(new com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership(dto.getServerId(), "MY_COMPLAINTS", currentPage));
                        }
                    }

                    android.util.Log.d("ComplaintRepo", String.format("MY_HTTP page=%d parsedCount=%d totalItems=%d totalPages=%d", 
                            currentPage, paginatedData.data.size(), totalItems, totalPages));

                    currentPage++;
                }

                java.util.Set<String> serverIdsReturned = new java.util.HashSet<>();
                for (com.voiceyanga.citizen.data.local.entity.ComplaintFeedMembership m : allMemberships) {
                    serverIdsReturned.add(m.getServerId());
                }
                java.util.List<Complaint> localSynced = complaintDao.getSyncedComplaintsByFeedType("MY_COMPLAINTS");
                if (localSynced != null) {
                    for (Complaint comp : localSynced) {
                        if (comp.getServerId() != null && !serverIdsReturned.contains(comp.getServerId())) {
                            complaintDao.purgeComplaintByServerId(comp.getServerId());
                        }
                    }
                }

                complaintDao.updateFeedMemberships("MY_COMPLAINTS", allMemberships);

                int observedCount = complaintDao.getMyCountSync();
                android.util.Log.d("ComplaintRepo", String.format("MY_DB upserted=%d membershipCount=%d observedCount=%d", 
                        accumulatedUpserted, allMemberships.size(), observedCount));

                myComplaintsState.postValue(new com.voiceyanga.citizen.feature.home.MyComplaintsState(
                    java.util.Collections.emptyList(),
                    false, false, true, false, null, currentPage - 1, totalPages, totalItems
                ));
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "My complaints refresh failed", e);
                com.voiceyanga.citizen.feature.home.MyComplaintsState s = myComplaintsState.getValue();
                String formattedError = com.voiceyanga.citizen.core.network.ErrorParser.parseThrowable(e);
                myComplaintsState.postValue(new com.voiceyanga.citizen.feature.home.MyComplaintsState(
                    s != null ? s.getItems() : java.util.Collections.emptyList(),
                    false, false, s != null && s.hasCompletedSuccessfulRefresh(),
                    s != null && s.hasCompletedSuccessfulRefresh(), formattedError, 
                    s != null ? s.getPage() : 0, s != null ? s.getTotalPages() : 0, s != null ? s.getTotalItems() : 0
                ));
            }
        });
    }

    public LiveData<List<Complaint>> observeMyComplaints(String userId, String email) {
        return complaintDao.getMyActiveComplaints(userId, email);
    }

    public void refreshComplaints() {
        refreshCommunityFeed();
        refreshMyComplaints();
    }

    private void logApiError(String action, Response<?> response) {
        if (response == null) return;
        String errorBody = "";
        try {
            if (response.errorBody() != null) errorBody = response.errorBody().string();
        } catch (Exception ignored) {}
        android.util.Log.e("ComplaintRepo", String.format("%s failed: Code=%d, SuccessField=%s, Body=%s", 
                action, response.code(), (response.body() instanceof ApiEnvelope ? ((ApiEnvelope<?>)response.body()).success : "N/A"), errorBody));
    }

    public void refreshComplaintDetail(String identifier) {
        executorService.execute(() -> {
            try {
                // Fetch specific complaint from server (using its UUID or serverId if known)
                Complaint local = complaintDao.getComplaintById(identifier);
                String serverId = (local != null && local.getServerId() != null) ? local.getServerId() : identifier;
                
                if (serverId != null) {
                    Response<com.google.gson.JsonElement> response = apiService.getComplaint(serverId).execute();
                    if (response.code() == 404) {
                        complaintDao.purgeComplaintByServerId(serverId);
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.JsonElement body = response.body();
                        ComplaintDto dto = null;
                        if (body.isJsonObject() && body.getAsJsonObject().has("data") && body.getAsJsonObject().has("success")) {
                            dto = gson.fromJson(body.getAsJsonObject().get("data"), ComplaintDto.class);
                        } else {
                            dto = gson.fromJson(body, ComplaintDto.class);
                        }
                        if (dto != null) {
                            processServerComplaints(Collections.singletonList(dto));
                            
                            // Get the final UUID to refresh comments correctly
                            Complaint updated = complaintDao.getComplaintById(serverId);
                            if (updated != null) {
                                refreshComments(serverId, updated.getClientUuid());
                            }
                        }
                    }
                } else {
                    // Fallback to full refresh if we don't have a server link
                    refreshComplaints();
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Detail refresh failed", e);
            }
        });
    }

    private int processServerComplaintsInternal(List<ComplaintDto> serverComplaints) {
        if (serverComplaints == null) {
            android.util.Log.d("ComplaintRepo", "processServerComplaints: Received null list");
            return 0;
        }
        
        android.util.Log.d("ComplaintRepo", "Processing " + serverComplaints.size() + " server complaints");
        java.util.List<Complaint> processedComplaints = new java.util.ArrayList<>();
        java.util.Map<String, java.util.List<com.voiceyanga.citizen.data.remote.dto.ComplaintMedia>> mediaMap = new java.util.HashMap<>();
        int upsertCount = 0;
        
        for (ComplaintDto dto : serverComplaints) {
            if (dto == null) continue;
            
            String serverId = dto.getServerId();
            if (serverId == null || serverId.isEmpty()) {
                android.util.Log.w("ComplaintRepo", "Skipping complaint with null ID");
                continue;
            }

            String targetUuid = dto.getClientUuid();
            
            if (targetUuid == null || targetUuid.isEmpty()) {
                Complaint serverMatch = complaintDao.getComplaintByServerId(serverId);
                if (serverMatch != null) {
                    targetUuid = serverMatch.getClientUuid();
                }
            }

            if (targetUuid == null || targetUuid.isEmpty()) {
                Complaint localMatch = complaintDao.findLocalPendingMatch(dto.getTitle(), dto.getAuthorEmail());
                if (localMatch != null) {
                    targetUuid = localMatch.getClientUuid();
                } else {
                    targetUuid = serverId;
                }
            }
            
            if (targetUuid == null || targetUuid.isEmpty()) continue;

            String localPreview = complaintDao.getFirstPhotoUri(targetUuid);
            Complaint existingLocal = complaintDao.getComplaintByUuid(targetUuid);
            
            String finalLocationName = "Unknown";
            if (existingLocal != null && existingLocal.getLocation() != null && !existingLocal.getLocation().trim().isEmpty() && !existingLocal.getLocation().equalsIgnoreCase("Unknown")) {
                finalLocationName = existingLocal.getLocation();
            } else if (dto.getLocation() != null && dto.getLocation().getDisplayName() != null && !dto.getLocation().getDisplayName().trim().isEmpty()) {
                finalLocationName = dto.getLocation().getDisplayName();
            }
            
            android.util.Log.d("ComplaintRepo", "Updating local complaint with UUID: " + targetUuid + ", location: " + finalLocationName);
            Complaint serverComplaint = new Complaint(
                    targetUuid,
                    dto.getTitle(),
                    dto.getDescription(),
                    dto.getCategory() != null ? dto.getCategory().getName() : "General",
                    finalLocationName,
                    "SYNCED",
                    dto.getCreatedAt()
            );
            
            serverComplaint.setServerId(serverId);
            serverComplaint.setReferenceCode(dto.getReferenceCode());
            serverComplaint.setUserId(dto.getUserId());
            serverComplaint.setStatus(dto.getStatus() != null ? dto.getStatus() : "SUBMITTED");
            serverComplaint.setAssignedTo(dto.getAssignedToName());
            serverComplaint.setAssignedOrganization(dto.getOrganizationName());
            serverComplaint.setAuthorEmail(dto.getAuthorEmail());
            
            if (dto.getVoiceNoteUrl() != null && !dto.getVoiceNoteUrl().isEmpty()) {
                serverComplaint.setVoiceNoteUrl(dto.getVoiceNoteUrl());
                serverComplaint.setVoiceNoteDuration(dto.getVoiceNoteDuration());
            }
            
            if (dto.getMedia() != null && !dto.getMedia().isEmpty()) {
                boolean firstPhotoSet = false;
                for (com.voiceyanga.citizen.data.remote.dto.ComplaintMedia m : dto.getMedia()) {
                    String mediaUrl = m.getFileUrl();
                    if ("AUDIO".equalsIgnoreCase(m.type) || "VOICE_NOTE".equalsIgnoreCase(m.type)) {
                        if (mediaUrl != null && !mediaUrl.isEmpty()) {
                            serverComplaint.setVoiceNoteUrl(mediaUrl);
                        }
                    } else if (m.type == null || "PHOTO".equalsIgnoreCase(m.type)) {
                        if (!firstPhotoSet && mediaUrl != null && !mediaUrl.isEmpty()) {
                            serverComplaint.setFirstPhotoUri(mediaUrl);
                            serverComplaint.setFirstPhotoMediaId(m.id);
                            firstPhotoSet = true;
                        }
                    } else if ("PROOF_OF_RESOLUTION".equalsIgnoreCase(m.type)) {
                        serverComplaint.setProofOfResolutionUri(mediaUrl);
                        serverComplaint.setProofOfResolutionMediaId(m.id);
                    }
                }
                if (!firstPhotoSet && localPreview != null && !localPreview.isEmpty()) {
                    serverComplaint.setFirstPhotoUri(localPreview);
                }
                mediaMap.put(targetUuid, dto.getMedia());
            } else {
                serverComplaint.setFirstPhotoUri(localPreview);
            }

            if (serverComplaint.isSupportedByMe() || (serverComplaint.getAuthorEmail() != null && serverComplaint.getAuthorEmail().equalsIgnoreCase(sessionManager.getUserEmail()))) {
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                            .subscribeToTopic("complaint_" + serverId);
                } catch (Exception ignored) {}
            }
            
            double serverLat = 0.0;
            double serverLon = 0.0;

            if (dto.getLocation() != null) {
                serverComplaint.setWard(dto.getLocation().getWard());
                serverComplaint.setDistrict(dto.getLocation().getDistrict());
                serverComplaint.setProvince(dto.getLocation().getProvince());
                serverComplaint.setLocationId(dto.getLocation().getId());
                if (dto.getLocation().getLatitude() != null && dto.getLocation().getLongitude() != null) {
                    serverLat = dto.getLocation().getLatitude();
                    serverLon = dto.getLocation().getLongitude();
                }
            }
            if (serverLat == 0.0 && dto.getLatitude() != 0.0) {
                serverLat = dto.getLatitude();
                serverLon = dto.getLongitude();
            }

            if (serverLat != 0.0 && serverLon != 0.0) {
                serverComplaint.setLatitude(serverLat);
                serverComplaint.setLongitude(serverLon);
                serverComplaint.setHasValidCoordinates(true);
            } else if (existingLocal != null && existingLocal.getLatitude() != 0.0) {
                serverComplaint.setLatitude(existingLocal.getLatitude());
                serverComplaint.setLongitude(existingLocal.getLongitude());
                serverComplaint.setHasValidCoordinates(existingLocal.isHasValidCoordinates());
            } else {
                serverComplaint.setHasValidCoordinates(false);
            }

            Complaint existing = complaintDao.getComplaintByUuid(targetUuid);
            if (existing != null) {
                // CRITICAL: Preserve local 'supportedByMe' state if a sync is pending
                // or if we just toggled it locally, to prevent server refresh overwriting UI state
                serverComplaint.setSupportedByMe(existing.isSupportedByMe());
                
                serverComplaint.setCommentCount(existing.getCommentCount());
                serverComplaint.setSupportCount(dto.getSupportCount());
                
                if (serverComplaint.getFirstPhotoUri() == null || serverComplaint.getFirstPhotoUri().isEmpty()) {
                    serverComplaint.setFirstPhotoUri(existing.getFirstPhotoUri());
                    serverComplaint.setFirstPhotoMediaId(existing.getFirstPhotoMediaId());
                }

                if (serverComplaint.getVoiceNoteUrl() == null || serverComplaint.getVoiceNoteUrl().isEmpty()) {
                    serverComplaint.setVoiceNoteUrl(existing.getVoiceNoteUrl());
                }
                if (existing.getVoiceNoteLocalPath() != null && !existing.getVoiceNoteLocalPath().isEmpty()) {
                    serverComplaint.setVoiceNoteLocalPath(existing.getVoiceNoteLocalPath());
                }
                if (serverComplaint.getVoiceNoteDuration() == 0 && existing.getVoiceNoteDuration() > 0) {
                    serverComplaint.setVoiceNoteDuration(existing.getVoiceNoteDuration());
                }

                // Engagement & Support / Comment Notification Logic
                String userEmail = sessionManager.getUserEmail();
                boolean userEngaged = existing.isSupportedByMe() 
                        || (userEmail != null && userEmail.equalsIgnoreCase(dto.getAuthorEmail()))
                        || complaintDao.getUserCommentCountOnComplaint(targetUuid, sessionManager.getUserName(), sessionManager.getUserEmail()) > 0;

                int oldCount = existing.getSupportCount();
                int newCount = dto.getSupportCount();
                if (newCount > oldCount && userEngaged) {
                    if (userEmail != null && Objects.equals(userEmail, dto.getAuthorEmail()) && newCount >= 10 && (newCount / 10) > (oldCount / 10)) {
                        int milestone = (newCount / 10) * 10;
                        notificationHelper.showMilestoneNotification(
                                "New Milestone Reached!",
                                dto.getTitle() != null ? dto.getTitle() : "Your Report",
                                milestone,
                                targetUuid
                        );
                    } else if (newCount > oldCount) {
                        notificationHelper.showNotification(
                                "New Support Received!",
                                "A report you engaged with (" + (dto.getTitle() != null ? dto.getTitle() : "Report") + ") received new community support (" + newCount + " votes).",
                                targetUuid,
                                "SUPPORT"
                        );
                    }
                }
            } else {
                serverComplaint.setSupportCount(dto.getSupportCount());
            }
            
            processedComplaints.add(serverComplaint);
            upsertCount++;
        }
        
        if (!processedComplaints.isEmpty()) {
            // CRITICAL: Insert parent complaints FIRST to satisfy Foreign Key constraints for photos
            complaintDao.insertAll(processedComplaints);
            for (Complaint c : processedComplaints) {
                complaintDao.update(c);
            }
            android.util.Log.d("ComplaintRepo", "Upserted " + upsertCount + " complaints to Room");

            // Now handle associated photos
            for (java.util.Map.Entry<String, java.util.List<com.voiceyanga.citizen.data.remote.dto.ComplaintMedia>> entry : mediaMap.entrySet()) {
                String uuid = entry.getKey();
                java.util.List<com.voiceyanga.citizen.data.remote.dto.ComplaintMedia> mediaList = entry.getValue();
                if (mediaList != null && !mediaList.isEmpty()) {
                    complaintDao.deletePhotosForComplaint(uuid);
                    for (com.voiceyanga.citizen.data.remote.dto.ComplaintMedia m : mediaList) {
                        String photoPath = m.getFileUrl();
                        if (photoPath != null && !photoPath.isEmpty() && (m.type == null || "PHOTO".equalsIgnoreCase(m.type))) {
                            ComplaintPhoto photo = new ComplaintPhoto(uuid, photoPath);
                            photo.setMediaId(m.id);
                            complaintDao.insertPhoto(photo);
                        }
                    }
                }
            }
        }
        return upsertCount;
    }

    private void processServerComplaints(List<ComplaintDto> serverComplaints) {
        processServerComplaintsInternal(serverComplaints);
    }

    public interface CommentsRefreshCallback {
        void onResult(com.voiceyanga.citizen.feature.complaints.ComplaintDetailViewModel.CommentsState state);
    }

    public void refreshComments(String serverId, String complaintUuid) {
        refreshComments(serverId, complaintUuid, null);
    }

    public void refreshComments(String serverId, String complaintUuid, CommentsRefreshCallback callback) {
        executorService.execute(() -> {
            try {
                Response<List<CommentResponse>> response = apiService.getComments(serverId).execute();
                int statusCode = response.code();
                
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentResponse> comments = response.body();
                    android.util.Log.d("ComplaintRepo", String.format("COMMENTS_HTTP serverIdPresent=true authAttached=true parsedCount=%d result=%d",
                            comments.size(), statusCode));
                            
                    Complaint local = complaintDao.getComplaintById(serverId);
                    String targetKey = (local != null && local.getClientUuid() != null) ? local.getClientUuid() : complaintUuid;
                    
                    complaintDao.clearSyncedCommentsForComplaint(targetKey);
                    if (serverId != null && !serverId.equals(targetKey)) {
                        complaintDao.clearSyncedCommentsForComplaint(serverId);
                    }

                    for (CommentResponse dto : comments) {
                        String author = dto.getAuthorName();
                        if (author == null || author.trim().isEmpty()) author = "Citizen";
                        
                        String content = dto.getContent();
                        if (content == null || content.trim().isEmpty()) content = "(No content)";

                        Comment comment = new Comment(
                                dto.getId() != null ? dto.getId() : java.util.UUID.randomUUID().toString(),
                                targetKey,
                                author,
                                content,
                                dto.isOfficial(),
                                dto.getCreatedAtMs()
                        );
                        complaintDao.insertComment(comment);
                    }
                    complaintDao.updateCommentCount(targetKey);
                    if (callback != null) callback.onResult(com.voiceyanga.citizen.feature.complaints.ComplaintDetailViewModel.CommentsState.SUCCESS);
                } else if (statusCode == 403) {
                    android.util.Log.e("ComplaintRepo", "COMMENTS_HTTP ownership=community serverIdPresent=true authAttached=true result=403");
                    if (callback != null) callback.onResult(com.voiceyanga.citizen.feature.complaints.ComplaintDetailViewModel.CommentsState.RESTRICTED);
                } else {
                    android.util.Log.e("ComplaintRepo", "COMMENTS_HTTP failed result=" + statusCode);
                    if (callback != null) callback.onResult(com.voiceyanga.citizen.feature.complaints.ComplaintDetailViewModel.CommentsState.ERROR);
                }
            } catch (Exception e) {
                android.util.Log.e("ComplaintRepo", "Comment refresh failed", e);
                if (callback != null) callback.onResult(com.voiceyanga.citizen.feature.complaints.ComplaintDetailViewModel.CommentsState.ERROR);
            }
        });
    }

    public void getHistory(String serverId, ReferenceCallback<List<HistoryItem>> callback) {
        apiService.getHistory(serverId).enqueue(new retrofit2.Callback<com.google.gson.JsonElement>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonElement> call, Response<com.google.gson.JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    com.google.gson.JsonElement body = response.body();
                    List<HistoryItem> items = null;
                    if (body.isJsonObject() && body.getAsJsonObject().has("data") && body.getAsJsonObject().has("success")) {
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<HistoryItem>>(){}.getType();
                        items = gson.fromJson(body.getAsJsonObject().get("data"), type);
                    } else {
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<HistoryItem>>(){}.getType();
                        items = gson.fromJson(body, type);
                    }
                    if (items != null) {
                        callback.onSuccess(items);
                    } else {
                        callback.onError("Failed to fetch history");
                    }
                } else {
                    callback.onError("Failed to fetch history");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonElement> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getMediaAccess(String serverId, String mediaId, ReferenceCallback<String> callback) {
        if (serverId == null || mediaId == null) {
            callback.onError("Missing IDs");
            return;
        }
        apiService.getMediaAccess(serverId, mediaId).enqueue(new retrofit2.Callback<ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.MediaAccessData>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.MediaAccessData>> call, Response<ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.MediaAccessData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onSuccess(response.body().data.url);
                } else {
                    callback.onError("Failed to get media access: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiEnvelope<com.voiceyanga.citizen.data.remote.dto.MediaAccessData>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private static final Object DATE_LOCK = new Object();
    public static long parseServerDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return System.currentTimeMillis();
        
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        };

        synchronized (DATE_LOCK) {
            for (String format : formats) {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                if (format.endsWith("'Z'")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                try {
                    Date date = sdf.parse(dateStr);
                    if (date != null) return date.getTime();
                } catch (ParseException ignored) {}
            }
        }

        return System.currentTimeMillis();
    }

    public interface ReferenceCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void postComment(String complaintUuid, String message) {
        executorService.execute(() -> {
            String author = sessionManager.getUserName();
            if (author == null || author.isEmpty()) author = "Anonymous User";
            
            Complaint local = complaintDao.getComplaintById(complaintUuid);
            String targetKey = (local != null && local.getClientUuid() != null) ? local.getClientUuid() : complaintUuid;

            Comment localComment = new Comment(
                    "local_" + UUID.randomUUID().toString(),
                    targetKey,
                    author,
                    message,
                    false,
                    System.currentTimeMillis()
            );
            complaintDao.insertComment(localComment);
            complaintDao.updateCommentCount(targetKey);

            complaintDao.insertPendingAction(new PendingAction("COMMENT", targetKey, message));
            scheduleSync(targetKey);
        });
    }

    public LiveData<List<Complaint>> getMyActiveComplaints(String userId, String email) {
        return complaintDao.getMyActiveComplaints(userId, email);
    }

    public LiveData<List<Complaint>> getMyResolvedComplaints(String userId, String email) {
        return complaintDao.getMyResolvedComplaints(userId, email);
    }

    public LiveData<List<Complaint>> getMySupportedComplaints(String userId, String email) {
        return complaintDao.getMySupportedComplaints(userId, email);
    }

    public LiveData<Complaint> getLatestMyComplaint(String userId, String email) {
        return complaintDao.getLatestMyComplaint(userId, email);
    }

    public LiveData<Integer> getMyReportsCount(String userId, String email) {
        return complaintDao.getMyReportsCount(userId, email);
    }

    public LiveData<Integer> getSupportedCount() {
        return complaintDao.getSupportedCount();
    }

    public LiveData<List<Complaint>> getOutboxComplaints() {
        return complaintDao.getOutboxComplaints();
    }

    public LiveData<List<Complaint>> getSuccessStories() {
        return complaintDao.getResolvedSuccessStories();
    }

    public LiveData<List<Complaint>> getCommunityComplaints(String email) {
        return getCommunityComplaints(null, null, null, null, null, null);
    }

    public LiveData<List<Complaint>> getCommunityComplaints(String status, String category, String query, String ward, String district, String province) {
        return complaintDao.getFilteredCommunityComplaints(status, category, query, ward, district, province);
    }

    public LiveData<List<Complaint>> getTrendingComplaints(String ward, String district) {
        return complaintDao.getTrendingComplaints(ward, district);
    }

    public LiveData<Complaint> getComplaint(String identifier) {
        return complaintDao.getComplaintByIdLiveData(identifier);
    }

    public Complaint getComplaintSync(String identifier) {
        return complaintDao.getComplaintById(identifier);
    }

    public LiveData<List<Comment>> getComments(String complaintUuid) {
        return complaintDao.getCommentsForComplaint(complaintUuid);
    }

    public LiveData<List<ComplaintPhoto>> getPhotos(String complaintUuid) {
        return complaintDao.getPhotosForComplaint(complaintUuid);
    }

    public List<ComplaintPhoto> getPhotosSync(String complaintUuid) {
        return complaintDao.getPhotosForComplaintSync(complaintUuid);
    }

    public void supportComplaint(String uuid) {
        executorService.execute(() -> {
            complaintDao.incrementSupportCount(uuid);
            complaintDao.insertPendingAction(new PendingAction("SUPPORT", uuid, null));
            Complaint complaint = complaintDao.getComplaintByUuid(uuid);
            if (complaint != null && complaint.getServerId() != null) {
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                            .subscribeToTopic("complaint_" + complaint.getServerId());
                    android.util.Log.d("ComplaintRepo", "Subscribed to FCM topic: complaint_" + complaint.getServerId());
                } catch (Exception e) {
                    android.util.Log.e("ComplaintRepo", "Failed to subscribe to FCM topic", e);
                }
            }
            scheduleSync(uuid);
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
            
            scheduleSync(complaint.getClientUuid());
        });
    }

    public void updateComplaint(Complaint complaint) {
        executorService.execute(() -> {
            complaintDao.update(complaint);
        });
    }

    public void scheduleSync() {
        scheduleSync("general");
    }

    public void scheduleSync(String clientUuid) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS)
                .addTag("complaint_sync")
                .addTag(clientUuid)
                .build();

        workManager.enqueueUniqueWork(
                "complaint-" + clientUuid,
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
                complaint.setFailureReason(null);
                complaintDao.update(complaint);
                scheduleSync();
            }
        });
    }

    public void deleteDraft() {
        executorService.execute(complaintDao::deleteDraft);
    }
}
