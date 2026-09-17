package com.voiceyanga.citizen.feature.complaints;

import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;
import com.voiceyanga.citizen.core.audio.VoiceNoteRecorder;
import com.voiceyanga.citizen.core.audio.VoiceNotePlayer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ComplaintViewModel extends androidx.lifecycle.AndroidViewModel {

    public static class VoiceNoteState {
        public File localFile = null;
        public boolean isRecording = false;
        public boolean isPlaying = false;
        public int elapsedSeconds = 0;
        public int durationSeconds = 0;
        public int amplitude = 0;
        public String error = null;
    }

    private final ComplaintRepository repository;
    private final ReferenceRepository referenceRepository;
    private final VoiceNoteRecorder recorder;
    private final VoiceNotePlayer player = new VoiceNotePlayer();

    private final MutableLiveData<VoiceNoteState> _voiceNoteState = new MutableLiveData<>(new VoiceNoteState());
    public LiveData<VoiceNoteState> getVoiceNoteState() { return _voiceNoteState; }

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
    
    private String currentClientUuid = null;
    private String lastSavedTitle = "";
    private String lastSavedDesc = "";
    private String lastSavedCategory = "";
    private String lastSavedLocation = "";

    private final MutableLiveData<LocationDto> _mappedLocation = new MutableLiveData<>();
    public LiveData<LocationDto> getMappedLocation() { return _mappedLocation; }

    public void mapToApiLocation(String district, String ward) {
        List<LocationDto> locations = _locations.getValue();
        if (locations == null) return;
        LocationDto fallback = null;
        for (LocationDto dto : locations) {
            boolean wardMatch = ward != null && dto.getWard() != null && dto.getWard().equalsIgnoreCase(ward);
            boolean districtMatch = district != null && dto.getDistrict() != null && dto.getDistrict().equalsIgnoreCase(district);
            if (wardMatch && districtMatch) {
                _mappedLocation.setValue(dto);
                return;
            } else if (wardMatch && fallback == null) {
                fallback = dto;
            } else if (districtMatch && fallback == null) {
                fallback = dto;
            }
        }
        if (fallback != null) {
            _mappedLocation.setValue(fallback);
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
        this.recorder = new VoiceNoteRecorder(application);
        setupVoiceNoteComponents();
        loadReferenceData();
        loadDraft();
    }

    private void setupVoiceNoteComponents() {
        recorder.setListener(new VoiceNoteRecorder.RecorderListener() {
            @Override
            public void onTimerTick(int seconds) {
                VoiceNoteState state = _voiceNoteState.getValue();
                if (state != null) {
                    state.elapsedSeconds = seconds;
                    _voiceNoteState.postValue(state);
                }
            }

            @Override
            public void onAmplitudeUpdate(int amplitude) {
                VoiceNoteState state = _voiceNoteState.getValue();
                if (state != null) {
                    state.amplitude = amplitude;
                    _voiceNoteState.postValue(state);
                }
            }

            @Override
            public void onAutoStop() {
                stopRecording();
            }

            @Override
            public void onError(String message) {
                VoiceNoteState state = _voiceNoteState.getValue();
                if (state != null) {
                    state.error = message;
                    state.isRecording = false;
                    _voiceNoteState.postValue(state);
                }
            }
        });

        player.setListener(new VoiceNotePlayer.PlayerListener() {
            @Override
            public void onCompletion() {
                VoiceNoteState state = _voiceNoteState.getValue();
                if (state != null) {
                    state.isPlaying = false;
                    _voiceNoteState.postValue(state);
                }
            }

            @Override
            public void onError(String message) {
                VoiceNoteState state = _voiceNoteState.getValue();
                if (state != null) {
                    state.error = message;
                    state.isPlaying = false;
                    _voiceNoteState.postValue(state);
                }
            }
        });
    }

    private File currentRecordingFile;

    public void startRecording() {
        File cacheDir = getApplication().getCacheDir();
        currentRecordingFile = new File(cacheDir, "voice_note_" + UUID.randomUUID() + ".m4a");
        
        VoiceNoteState state = _voiceNoteState.getValue();
        if (state != null) {
            state.isRecording = true;
            state.elapsedSeconds = 0;
            state.localFile = null;
            state.error = null;
            _voiceNoteState.setValue(state);
        }
        
        recorder.start(currentRecordingFile);
    }

    public void stopRecording() {
        recorder.stop();
        VoiceNoteState state = _voiceNoteState.getValue();
        if (state != null) {
            state.isRecording = false;
            state.durationSeconds = state.elapsedSeconds;
            
            // Check if recording is long enough (min 2s)
            if (state.elapsedSeconds < 2) {
                state.error = getApplication().getString(R.string.error_recording_too_short);
                state.localFile = null;
                if (currentRecordingFile != null && currentRecordingFile.exists()) {
                    boolean deleted = currentRecordingFile.delete();
                    if (!deleted) android.util.Log.w("ComplaintVM", "Could not delete short recording file");
                }
            } else {
                state.localFile = currentRecordingFile;
            }
            _voiceNoteState.postValue(state);
        }
    }

    public void playRecording() {
        VoiceNoteState state = _voiceNoteState.getValue();
        if (state != null && state.localFile != null && state.localFile.exists()) {
            state.isPlaying = true;
            state.error = null;
            _voiceNoteState.setValue(state);
            player.play(state.localFile.getAbsolutePath());
        } else if (state != null) {
            state.error = getApplication().getString(R.string.error_file_not_found);
            _voiceNoteState.setValue(state);
        }
    }

    public void pauseRecording() {
        VoiceNoteState state = _voiceNoteState.getValue();
        if (state != null) {
            state.isPlaying = false;
            _voiceNoteState.setValue(state);
            player.pause();
        }
    }

    public void deleteRecording() {
        VoiceNoteState state = _voiceNoteState.getValue();
        if (state != null && state.localFile != null) {
            if (state.localFile.exists()) {
                state.localFile.delete();
            }
            state.localFile = null;
            state.durationSeconds = 0;
            state.elapsedSeconds = 0;
            state.isPlaying = false;
            _voiceNoteState.setValue(state);
            player.stop();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recorder.release();
        player.release();
    }

    public void loadDraft() {
        new Thread(() -> {
            Complaint draft = repository.getDraftSync();
            if (draft != null) {
                currentClientUuid = draft.getClientUuid();
                _draft.postValue(draft);
                loadMediaForComplaint(draft);
            } else {
                currentClientUuid = UUID.randomUUID().toString();
            }
        }).start();
    }

    public void loadComplaintForEdit(String uuid) {
        this.currentClientUuid = uuid;
        new Thread(() -> {
            Complaint complaint = repository.getComplaintSync(uuid);
            if (complaint != null) {
                _draft.postValue(complaint);
                loadMediaForComplaint(complaint);
            }
        }).start();
    }

    private void loadMediaForComplaint(Complaint complaint) {
        if (complaint.getVoiceNoteLocalPath() != null) {
            File file = new File(complaint.getVoiceNoteLocalPath());
            if (file.exists()) {
                VoiceNoteState state = new VoiceNoteState();
                state.localFile = file;
                state.durationSeconds = complaint.getVoiceNoteDuration();
                _voiceNoteState.postValue(state);
            }
        }
        
        List<com.voiceyanga.citizen.data.local.entity.ComplaintPhoto> photos = repository.getPhotosSync(complaint.getClientUuid());
        if (photos != null) {
            java.util.List<String> uris = new java.util.ArrayList<>();
            for (com.voiceyanga.citizen.data.local.entity.ComplaintPhoto p : photos) {
                uris.add(p.getPhotoUri());
                if (p.getLabel() != null) photoLabels.put(p.getPhotoUri(), p.getLabel());
            }
            _selectedPhotos.postValue(uris);
        }
    }

    public void saveDraft(String title, String description, CategoryDto category, String customCategory, LocationDto location, String addressString) {
        String otherCategory = getApplication().getString(R.string.category_other);
        String categoryName = (category != null && !category.getName().equalsIgnoreCase(otherCategory)) 
            ? category.getName() 
            : (customCategory != null && !customCategory.isEmpty() ? customCategory : otherCategory);
            
        String finalLocation = (addressString != null && !addressString.isEmpty()) ? addressString : 
                              (location != null ? location.getDisplayName() : null);

        // Dirty check
        if (Objects.equals(title, lastSavedTitle) && 
            Objects.equals(description, lastSavedDesc) &&
            Objects.equals(categoryName, lastSavedCategory) &&
            Objects.equals(finalLocation, lastSavedLocation)) {
            return;
        }

        lastSavedTitle = title;
        lastSavedDesc = description;
        lastSavedCategory = categoryName;
        lastSavedLocation = finalLocation;

        Complaint draft = new Complaint(
                currentClientUuid,
                title != null ? title : "",
                description != null ? description : "",
                categoryName,
                finalLocation,
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

    public void submitComplaint(String title, String description, CategoryDto category, String customCategory, LocationDto location, String addressString, double lat, double lon) {
        VoiceNoteState vnState = _voiceNoteState.getValue();
        boolean hasVoiceNote = vnState != null && vnState.localFile != null;
        
        String otherCategory = getApplication().getString(R.string.category_other);
        String categoryName = (category != null && !category.getName().equalsIgnoreCase(otherCategory)) 
            ? category.getName() 
            : (customCategory != null && !customCategory.isEmpty() ? customCategory : null);

        String finalLocation = (addressString != null && !addressString.trim().isEmpty()) ? addressString : 
                              (location != null ? location.getDisplayName() : getApplication().getString(R.string.default_location));

        if (currentClientUuid == null) {
            currentClientUuid = UUID.randomUUID().toString();
        }

        // Use the same validation logic as the request model
        com.voiceyanga.citizen.data.remote.dto.ComplaintRequest validationRequest = 
            new com.voiceyanga.citizen.data.remote.dto.ComplaintRequest(
                title,
                description,
                categoryName,
                finalLocation,
                "MEDIUM",
                currentClientUuid
            );
        
        if (hasVoiceNote) {
            validationRequest.voiceNoteUrl = "temp"; // Placeholder for validation
        }

        if (!validationRequest.isValid()) {
            if (title == null || title.trim().length() < 5) {
                _error.setValue("Title must be at least 5 characters");
            } else if (title.length() > 150) {
                _error.setValue("Title must not exceed 150 characters");
            } else if (categoryName == null || categoryName.isEmpty()) {
                _error.setValue(getApplication().getString(R.string.error_select_problem_type));
            } else if (!hasVoiceNote && (description == null || description.trim().length() < 10)) {
                _error.setValue("Description must be at least 10 characters or include a voice note");
            } else {
                _error.setValue("Invalid complaint data. Please check all fields.");
            }
            return;
        }

        List<String> photos = _selectedPhotos.getValue();
        if (photos != null && photos.size() > 5) {
            _error.setValue("Maximum 5 photos allowed");
            return;
        }

        _loading.setValue(true);
        
        // Remove common numeric debris if found at the start of string
        finalLocation = finalLocation.replaceAll("^[A-Z0-9]{4}\\+[A-Z0-9]{2,3}\\s*,*\\s*", "");

        Complaint complaint = new Complaint(
                currentClientUuid,
                title,
                description != null ? description : "",
                categoryName != null ? categoryName : getApplication().getString(R.string.default_category),
                finalLocation,
                "PENDING",
                System.currentTimeMillis()
        );
        complaint.setLatitude(lat);
        complaint.setLongitude(lon);
        complaint.setUserId(repository.getUserId());
        
        if (category != null && !category.getName().equalsIgnoreCase(otherCategory)) {
            complaint.setCategoryId(category.getId());
        }

        if (hasVoiceNote) {
            complaint.setVoiceNoteLocalPath(vnState.localFile.getAbsolutePath());
            complaint.setVoiceNoteDuration(vnState.durationSeconds);
        }
        
        complaint.setFailureReason(null); // Clear previous errors if any
        
        repository.deleteDraft(); // Clear any existing draft before promoting
        repository.saveComplaint(complaint, _selectedPhotos.getValue(), photoLabels);
        
        // Immediate feedback [UX-FIX]
        _loading.setValue(false);
        _submissionSuccess.setValue(true);
        
        // Clear mapped location after success
        _mappedLocation.setValue(null);
    }

    public void addPhoto(String uriString) {
        List<String> current = _selectedPhotos.getValue();
        if (current != null && current.size() < 5) {
            try {
                android.net.Uri uri = android.net.Uri.parse(uriString);
                if (uriString.startsWith("content://")) {
                    // Copy to internal storage to ensure permanent access for sync and sharing
                    java.io.File storageDir = new java.io.File(getApplication().getFilesDir(), "photos");
                    if (!storageDir.exists() && !storageDir.mkdirs()) {
                        android.util.Log.w("ComplaintVM", "Could not create photos directory");
                    }
                    
                    java.io.File localFile = new java.io.File(storageDir, "IMG_" + UUID.randomUUID() + ".jpg");
                    java.io.InputStream is = getApplication().getContentResolver().openInputStream(uri);
                    java.io.FileOutputStream os = new java.io.FileOutputStream(localFile);
                    
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    is.close();
                    os.close();
                    
                    current.add(localFile.getAbsolutePath());
                } else {
                    current.add(uriString);
                }
                _selectedPhotos.setValue(current);
            } catch (java.io.IOException e) {
                android.util.Log.e("ComplaintVM", "Failed to copy photo", e);
                _error.setValue("Failed to process photo selection.");
            }
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