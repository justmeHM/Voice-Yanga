package com.voiceyanga.citizen.feature.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.remote.dto.UserDto;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import com.voiceyanga.citizen.data.repository.UserRepository;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;
    private final UserRepository userRepository;
    private final ReferenceRepository referenceRepository;
    private final NotificationRepository notificationRepository;
    
    private final MutableLiveData<Map<String, String>> filters = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> sortOrder = new MutableLiveData<>("NEWEST");
    
    public LiveData<Map<String, String>> getFilters() { return filters; }

    private final androidx.lifecycle.MediatorLiveData<List<Complaint>> _sortedComplaints = new androidx.lifecycle.MediatorLiveData<>();
    public LiveData<List<Complaint>> getComplaints() { return _sortedComplaints; }

    private final androidx.lifecycle.MediatorLiveData<List<Complaint>> _trendingComplaints = new androidx.lifecycle.MediatorLiveData<>();
    public LiveData<List<Complaint>> getTrendingComplaints() { return _trendingComplaints; }

    private final androidx.lifecycle.MediatorLiveData<List<Complaint>> _successStories = new androidx.lifecycle.MediatorLiveData<>();
    public LiveData<List<Complaint>> getSuccessStories() { return _successStories; }

    private final LiveData<List<Complaint>> trendingSource;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<com.voiceyanga.citizen.feature.home.CommunityFeedState> getCommunityFeedState() {
        return repository.getCommunityFeedState();
    }

    @Inject
    public HomeViewModel(ComplaintRepository repository, SessionManager sessionManager,
                         UserRepository userRepository,
                         ReferenceRepository referenceRepository,
                         NotificationRepository notificationRepository) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        this.userRepository = userRepository;
        this.referenceRepository = referenceRepository;
        this.notificationRepository = notificationRepository;

        _sortedComplaints.addSource(repository.getCommunityFeedState(), state -> {
            if (state != null) {
                _loading.setValue(state.isInitialLoading() || state.isRefreshing());
                _error.setValue(state.getError());
            }
        });

        // Start with no automatic filters on launch to see all complaints by default
        // loadUserProfile();

        // Chain filters and sorting
        LiveData<List<Complaint>> filteredComplaints = Transformations.switchMap(filters, f -> {
            String status = f.get("status");
            String category = f.get("category");
            String search = f.get("search");
            String ward = f.get("ward");
            String district = f.get("district");
            String province = f.get("province");
            return repository.observeCommunityFeed(status, category, search, ward, district, province);
        });

        _sortedComplaints.addSource(filteredComplaints, list -> performSort(list, sortOrder.getValue()));
        _sortedComplaints.addSource(sortOrder, order -> performSort(filteredComplaints.getValue(), order));

        this.trendingSource = Transformations.switchMap(filters, f -> {
            String district = f.get("district");
            return repository.getTrendingComplaints(district);
        });

        _trendingComplaints.addSource(trendingSource, list -> {
            _trendingComplaints.setValue(list);
        });

        _successStories.addSource(repository.getSuccessStories(), list -> {
            if (list != null) {
                java.util.List<Complaint> sneakPeek = new java.util.ArrayList<>();
                int topCount = Math.min(list.size(), 5);
                for (int i = 0; i < topCount; i++) {
                    sneakPeek.add(list.get(i));
                }
                _successStories.setValue(sneakPeek);
            }
        });
    }

    private void loadUserProfile() {
        userRepository.getProfile(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String locationId = response.body().getLocationId();
                    if (locationId != null) {
                        applyDefaultLocationFilter(locationId);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {}
        });
    }

    private void applyDefaultLocationFilter(String locationId) {
        referenceRepository.getLocations(new ReferenceRepository.ReferenceCallback<List<LocationDto>>() {
            @Override
            public void onSuccess(List<LocationDto> data) {
                for (LocationDto loc : data) {
                    if (Objects.equals(loc.getId(), locationId)) {
                        // User's default is their city/district
                        setFilter("district", loc.getDistrict());
                        break;
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void performSort(List<Complaint> list, String order) {
        executor.execute(() -> {
            if (list == null) {
                _sortedComplaints.postValue(null);
                _loading.postValue(false);
                return;
            }

            java.util.List<Complaint> sorted = new java.util.ArrayList<>(list);
            String finalOrder = (order != null) ? order : "NEWEST";
            
            switch (finalOrder) {
                case "SUPPORT":
                    sorted.sort((c1, c2) -> Integer.compare(c2.getSupportCount(), c1.getSupportCount()));
                    break;
                case "NEWEST":
                default:
                    sorted.sort((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    break;
            }
            _sortedComplaints.postValue(sorted);
            _loading.postValue(false);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    public void setSortOrder(String order) {
        sortOrder.setValue(order);
    }

    public void setFilter(String key, String value) {
        Map<String, String> currentFilters = new HashMap<>(filters.getValue() != null ? filters.getValue() : Collections.emptyMap());
        if (value == null || value.isEmpty()) {
            currentFilters.remove(key);
        } else {
            currentFilters.put(key, value);
        }
        filters.setValue(currentFilters);
    }

    public void clearFilters() {
        filters.setValue(Collections.emptyMap());
    }

    public void refreshData() {
        _loading.setValue(true);
        repository.refreshCommunityFeed();
        notificationRepository.refreshNotifications();
        
        // Trigger filters update to refresh the LiveData stream
        Map<String, String> current = filters.getValue();
        filters.setValue(current != null ? current : new java.util.HashMap<>());
    }

    public void retrySync() {
        repository.scheduleSync();
    }

    public void retryComplaint(String uuid) {
        repository.retryComplaint(uuid);
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public void setLoading(boolean loading) {
        _loading.postValue(loading);
    }

    public LiveData<Complaint> getLatestMyComplaint() {
        return repository.getLatestMyComplaint(sessionManager.getUserId(), sessionManager.getUserEmail());
    }

    public LiveData<Complaint> getDraft() {
        MutableLiveData<Complaint> draftData = new MutableLiveData<>();
        new Thread(() -> {
            draftData.postValue(repository.getDraftSync());
        }).start();
        return draftData;
    }

    public LiveData<Integer> getMyReportsCount() {
        return repository.getMyReportsCount(sessionManager.getUserId(), sessionManager.getUserEmail());
    }

    public LiveData<Integer> getSupportedCount() {
        return repository.getSupportedCount();
    }

    public LiveData<List<Complaint>> getOutboxComplaints() {
        return repository.getOutboxComplaints();
    }

    public LiveData<Integer> getUnreadNotificationCount() {
        return notificationRepository.getUnreadCount();
    }
}
