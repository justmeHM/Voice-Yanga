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
    
    private final MutableLiveData<List<Complaint>> _sortedComplaints = new MutableLiveData<>();
    public LiveData<List<Complaint>> getComplaints() { return _sortedComplaints; }

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

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

        // Observe filteredComplaints and trigger sort
        filteredComplaints.observeForever(list -> performSort(list, sortOrder.getValue()));
        sortOrder.observeForever(order -> performSort(filteredComplaints.getValue(), order));
    }

    private void performSort(List<Complaint> list, String order) {
        if (list == null) {
            _sortedComplaints.postValue(null);
            return;
        }
        
        executor.execute(() -> {
            java.util.List<Complaint> sorted = new java.util.ArrayList<>(list);
            switch (order != null ? order : "NEWEST") {
                case "SUPPORT":
                    sorted.sort((c1, c2) -> Integer.compare(c2.getSupportCount(), c1.getSupportCount()));
                    break;
                case "NEWEST":
                default:
                    sorted.sort((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
                    break;
            }
            _sortedComplaints.postValue(sorted);
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

    public void retrySync() {
        repository.scheduleSync();
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public void setLoading(boolean loading) {
        _loading.postValue(loading);
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
