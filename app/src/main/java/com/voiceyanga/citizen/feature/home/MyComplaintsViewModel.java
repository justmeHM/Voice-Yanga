package com.voiceyanga.citizen.feature.home;

import androidx.lifecycle.LiveData;
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

    @Inject
    public MyComplaintsViewModel(ComplaintRepository repository, SessionManager sessionManager) {
        this.repository = repository;
        this.sessionManager = sessionManager;
    }

    public LiveData<List<Complaint>> getMyComplaints() {
        return repository.getMyComplaints(sessionManager.getUserEmail());
    }
}