package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.UUID;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintViewModel extends ViewModel {

    private final ComplaintRepository repository;
    private final MutableLiveData<Boolean> _submissionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getSubmissionSuccess() { return _submissionSuccess; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public LiveData<Boolean> getLoading() { return _loading; }

    @Inject
    public ComplaintViewModel(ComplaintRepository repository) {
        this.repository = repository;
    }

    public void submitComplaint(String title, String description, String category, String location) {
        if (title.isEmpty() || description.isEmpty() || category == null) {
            _error.setValue("Please fill all required fields");
            return;
        }

        _loading.setValue(true);

        Complaint complaint = new Complaint(
                UUID.randomUUID().toString(),
                title,
                description,
                category,
                location,
                "PENDING",
                System.currentTimeMillis()
        );

        repository.saveComplaint(complaint);
        
        // Simulate a slight delay for UI feedback
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            _loading.setValue(false);
            _submissionSuccess.setValue(true);
        }, 800);
    }
}