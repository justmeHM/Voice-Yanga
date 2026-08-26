package com.voiceyanga.citizen.feature.complaints;

import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintViewModel extends androidx.lifecycle.AndroidViewModel {

    private final ComplaintRepository repository;
    private final ReferenceRepository referenceRepository;

    private final MutableLiveData<List<CategoryDto>> _categories = new MutableLiveData<>();
    public LiveData<List<CategoryDto>> getCategories() { return _categories; }

    private final MutableLiveData<List<LocationDto>> _locations = new MutableLiveData<>();
    public LiveData<List<LocationDto>> getLocations() { return _locations; }

    private final MutableLiveData<Boolean> _submissionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getSubmissionSuccess() { return _submissionSuccess; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<List<String>> _selectedPhotos = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getSelectedPhotos() { return _selectedPhotos; }

    private final java.util.Map<String, String> photoLabels = new java.util.HashMap<>();

    public void updatePhotoLabel(String uri, String label) {
        photoLabels.put(uri, label);
    }

    private final MutableLiveData<Complaint> _draft = new MutableLiveData<>();
    public LiveData<Complaint> getDraft() { return _draft; }

    private final MutableLiveData<LocationDto> _mappedLocation = new MutableLiveData<>();
    public LiveData<LocationDto> getMappedLocation() { return _mappedLocation; }

    public void mapToApiLocation(String district, String ward) {
        List<LocationDto> locations = _locations.getValue();
        if (locations == null) return;
        for (LocationDto dto : locations) {
            if ((district != null && dto.getDistrict().equalsIgnoreCase(district)) || 
                (ward != null && dto.getWard().equalsIgnoreCase(ward))) {
                _mappedLocation.setValue(dto);
                break;
            }
        }
    }

    public void setManualLocation(LocationDto location) {
        _mappedLocation.setValue(location);
    }

    @Inject
    public ComplaintViewModel(@NonNull Application application, ComplaintRepository repository, ReferenceRepository referenceRepository) {
        super(application);
        this.repository = repository;
        this.referenceRepository = referenceRepository;
        loadReferenceData();
        loadDraft();
    }

    private void loadDraft() {
        new Thread(() -> {
            Complaint draft = repository.getDraftSync();
            if (draft != null) {
                _draft.postValue(draft);
            }
        }).start();
    }

    public void saveDraft(String title, String description, CategoryDto category, String customCategory, LocationDto location) {
        String categoryName = (category != null && !category.getName().equalsIgnoreCase("Other")) 
            ? category.getName() 
            : (customCategory != null && !customCategory.isEmpty() ? customCategory : "Other");
            
        Complaint draft = new Complaint(
                UUID.randomUUID().toString(),
                title,
                description,
                categoryName,
                location != null ? location.getDisplayName() : null,
                "DRAFT",
                System.currentTimeMillis()
        );
        repository.saveAsDraft(draft);
    }

    public void deleteDraft() {
        repository.deleteDraft();
    }

    private void loadReferenceData() {
        referenceRepository.getCategories(new ReferenceRepository.ReferenceCallback<List<CategoryDto>>() {
            @Override
            public void onSuccess(List<CategoryDto> data) {
                _categories.setValue(data);
            }

            @Override
            public void onError(String message) {
                _error.setValue(message);
            }
        });

        referenceRepository.getLocations(new ReferenceRepository.ReferenceCallback<List<LocationDto>>() {
            @Override
            public void onSuccess(List<LocationDto> data) {
                _locations.setValue(data);
            }

            @Override
            public void onError(String message) {
                _error.setValue(message);
            }
        });
    }

    public void submitComplaint(String title, String description, CategoryDto category, String customCategory, LocationDto location, double lat, double lon) {
        if (title.isEmpty() || description.length() < 10) {
            _error.setValue(description.isEmpty() ? 
                getApplication().getString(R.string.error_fill_fields) : 
                "Description must be at least 10 characters");
            return;
        }

        String categoryName = (category != null && !category.getName().equalsIgnoreCase("Other")) 
            ? category.getName() 
            : (customCategory != null && !customCategory.isEmpty() ? customCategory : null);
            
        if (category == null && (customCategory == null || customCategory.isEmpty())) {
            _error.setValue("Please select a category or specify one under 'Other'");
            return;
        }
        
        if (category != null && category.getName().equalsIgnoreCase("Other") && (customCategory == null || customCategory.isEmpty())) {
             _error.setValue("Please specify the problem type for 'Other'");
             return;
        }

        _loading.setValue(true);

        Complaint complaint = new Complaint(
                UUID.randomUUID().toString(),
                title,
                description,
                categoryName != null ? categoryName : "General",
                location != null ? location.getDisplayName() : "Lusaka",
                "PENDING",
                System.currentTimeMillis()
        );
        complaint.setLatitude(lat);
        complaint.setLongitude(lon);
        
        if (category != null && !category.getName().equalsIgnoreCase("Other")) {
            complaint.setCategoryId(category.getId());
        }
        
        repository.saveComplaint(complaint, _selectedPhotos.getValue(), photoLabels);
        
        // Simulate a slight delay for UI feedback
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            _loading.setValue(false);
            _submissionSuccess.setValue(true);
        }, 800);
        
        // Clear mapped location after success
        _mappedLocation.setValue(null);
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