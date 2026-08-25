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

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;
    private final MutableLiveData<Boolean> showResolved = new MutableLiveData<>(false);
    private final LiveData<List<Complaint>> complaints;
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    @Inject
    public MyComplaintsViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
        this.complaints = Transformations.switchMap(showResolved, resolved -> {
            _loading.setValue(true);
            String email = sessionManager.getUserEmail();
            if (resolved) {
                return repository.getMyResolvedComplaints(email);
            } else {
                return repository.getMyActiveComplaints(email);
            }
        });
    }

    public LiveData<List<Complaint>> getMyComplaints() {
        return complaints;
    }

    public void setShowResolved(boolean resolved) {
        showResolved.setValue(resolved);
    }

    public LiveData<Integer> getMyReportsCount() {
        return repository.getMyReportsCount(sessionManager.getUserEmail());
    }

    public LiveData<Integer> getSupportedCount() {
        return repository.getSupportedCount();
    }
}
