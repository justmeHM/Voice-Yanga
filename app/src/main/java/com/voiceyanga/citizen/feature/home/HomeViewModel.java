package com.voiceyanga.citizen.feature.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;
    private final MutableLiveData<Map<String, String>> filters = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> sortOrder = new MutableLiveData<>("NEWEST");
    private final LiveData<List<Complaint>> complaints;
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    @Inject
    public HomeViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;

        // Chain filters and sorting
        LiveData<List<Complaint>> filteredComplaints = Transformations.switchMap(filters, f -> {
            _loading.setValue(true);
            String status = f.get("status");
            String category = f.get("category");
            String search = f.get("search");
            String email = sessionManager.getUserEmail();
            return repository.getCommunityComplaints(email, status, category, search);
        });

        this.complaints = Transformations.switchMap(sortOrder, order -> 
            Transformations.map(filteredComplaints, list -> {
                if (list == null) return null;
                java.util.List<Complaint> sorted = new java.util.ArrayList<>(list);
                switch (order) {
                    case "SUPPORT":
                        sorted.sort((c1, c2) -> Integer.compare(c2.getSupportCount(), c1.getSupportCount()));
                        break;
                    case "CLOSEST":
                        // Closest logic would need user location, for now by lat/lon if available
                        break;
                    case "NEWEST":
                    default:
                        sorted.sort((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                        break;
                }
                return sorted;
            })
        );
    }

    public void setSortOrder(String order) {
        sortOrder.setValue(order);
    }

    public LiveData<List<Complaint>> getComplaints() {
        return complaints;
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

    public void retrySync() {
        repository.scheduleSync();
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public LiveData<Complaint> getLatestMyComplaint() {
        return repository.getLatestMyComplaint(sessionManager.getUserEmail());
    }

    public LiveData<Complaint> getDraft() {
        MutableLiveData<Complaint> draftData = new MutableLiveData<>();
        new Thread(() -> {
            draftData.postValue(repository.getDraftSync());
        }).start();
        return draftData;
    }

    public LiveData<Integer> getMyReportsCount() {
        return repository.getMyReportsCount(sessionManager.getUserEmail());
    }

    public LiveData<Integer> getSupportedCount() {
        return repository.getSupportedCount();
    }
}