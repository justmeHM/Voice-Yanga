package com.voiceyanga.citizen.feature.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final LiveData<List<Complaint>> complaints;

    @Inject
    public HomeViewModel(ComplaintRepository repository) {
        this.repository = repository;
        this.complaints = repository.getAllComplaints();
    }

    public LiveData<List<Complaint>> getComplaints() {
        return complaints;
    }

    public void retrySync() {
        repository.scheduleSync();
    }
}