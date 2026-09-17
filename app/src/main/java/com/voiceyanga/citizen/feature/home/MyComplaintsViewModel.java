package com.voiceyanga.citizen.feature.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MyComplaintsViewModel extends ViewModel {

    public enum Tab {
        ACTIVE, RESOLVED, SUPPORTED
    }

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;
    private final MutableLiveData<Tab> selectedTab = new MutableLiveData<>(Tab.ACTIVE);
    private final androidx.lifecycle.MediatorLiveData<List<Complaint>> complaints = new androidx.lifecycle.MediatorLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    
    public LiveData<Boolean> getLoading() { return _loading; }
    public LiveData<String> getError() { return _error; }
    public LiveData<com.voiceyanga.citizen.feature.home.MyComplaintsState> getMyComplaintsState() { return repository.getMyComplaintsState(); }

    @Inject
    public MyComplaintsViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        
        LiveData<List<Complaint>> tabSource = Transformations.switchMap(selectedTab, tab -> {
            String userId = sessionManager.getUserId();
            String email = sessionManager.getUserEmail();
            switch (tab) {
                case RESOLVED:
                    return repository.getMyResolvedComplaints(userId, email);
                case SUPPORTED:
                    return repository.getMySupportedComplaints(userId, email);
                case ACTIVE:
                default:
                    return repository.observeMyComplaints(userId, email);
            }
        });
        complaints.addSource(tabSource, complaints::setValue);
        complaints.addSource(repository.getMyComplaintsState(), state -> {
            if (state != null) {
                _loading.setValue(state.isInitialLoading() || state.isRefreshing());
                _error.setValue(state.getError());
            }
        });
    }

    public LiveData<List<Complaint>> getMyComplaints() {
        return complaints;
    }

    public void setSelectedTab(Tab tab) {
        selectedTab.setValue(tab);
    }

    public void retrySync(String uuid) {
        repository.retryComplaint(uuid);
    }

    public void setLoading(boolean loading) {
        _loading.setValue(loading);
    }

    public void refreshData() {
        _loading.setValue(true);
        repository.refreshMyComplaints();
    }

    public LiveData<Integer> getMyReportsCount() {
        return repository.getMyReportsCount(sessionManager.getUserId(), sessionManager.getUserEmail());
    }

    public LiveData<Integer> getSupportedCount() {
        return repository.getSupportedCount();
    }
}
