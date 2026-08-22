package com.voiceyanga.citizen.feature.complaints;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import android.app.Application;
import androidx.annotation.NonNull;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintViewModel extends androidx.lifecycle.AndroidViewModel {

    private final ComplaintRepository repository;
    private final MutableLiveData<Boolean> _submissionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getSubmissionSuccess() { return _submissionSuccess; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<List<String>> _selectedPhotos = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getSelectedPhotos() { return _selectedPhotos; }

    @Inject
    public ComplaintViewModel(@NonNull Application application, ComplaintRepository repository) {
        super(application);
        this.repository = repository;
    }

    public void submitComplaint(String title, String description, String category, String location) {
        if (title.isEmpty() || description.isEmpty() || category == null) {
            _error.setValue(getApplication().getString(R.string.error_fill_fields));
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

        repository.saveComplaint(complaint, _selectedPhotos.getValue());
        
        // Simulate a slight delay for UI feedback
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            _loading.setValue(false);
            _submissionSuccess.setValue(true);
        }, 800);
    }

    public void addPhoto(String uri) {
        List<String> current = _selectedPhotos.getValue();
        if (current != null && current.size() < 5) {
            current.add(uri);
            _selectedPhotos.setValue(current);
        } else {
            _error.setValue(getApplication().getString(R.string.error_max_photos));
        }
    }

    public void removePhoto(String uri) {
        List<String> current = _selectedPhotos.getValue();
        if (current != null) {
            current.remove(uri);
            _selectedPhotos.setValue(current);
        }
    }
}