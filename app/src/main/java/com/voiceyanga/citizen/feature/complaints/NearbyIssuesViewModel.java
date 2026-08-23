package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NearbyIssuesViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final SessionManager sessionManager;

    @Inject
    public NearbyIssuesViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public LiveData<List<Complaint>> getNearbyComplaints() {
        // Filter out complaints created by the current user to show only community issues
        return repository.getCommunityComplaints(sessionManager.getUserEmail());
    }

    public void supportComplaint(String uuid) {
        repository.supportComplaint(uuid);
    }

    public void retrySync() {
        repository.scheduleSync();
    }
}
