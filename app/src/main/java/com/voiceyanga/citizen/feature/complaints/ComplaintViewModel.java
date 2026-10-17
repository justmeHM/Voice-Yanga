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
    private Complaint editingComplaint = null;
    private String lastSavedTitle = "";
    private String lastSavedDesc = "";
    private String lastSavedCategory = "";
    private String lastSavedLocation = "";

    private final MutableLiveData<LocationDto> _mappedLocation = new MutableLiveData<>();
    public LiveData<LocationDto> getMappedLocation() { return _mappedLocation; }

    public void mapToApiLocation(String district, String ward) {
        List<LocationDto> locations = _locations.getValue();
        if (locations == null || locations.isEmpty()) return;

        for (LocationDto dto : locations) {
            boolean wardMatch = ward != null && dto.getWard() != null && dto.getWard().equalsIgnoreCase(ward);
            boolean districtMatch = district != null && dto.getDistrict() != null && dto.getDistrict().equalsIgnoreCase(district);
            if (wardMatch && districtMatch) {
                _mappedLocation.setValue(dto);
                return;
            }
        }
        
        for (LocationDto dto : locations) {
            boolean wardMatch = ward != null && dto.getWard() != null && dto.getWard().equalsIgnoreCase(ward);
            if (wardMatch) {
                _mappedLocation.setValue(dto);
                return;
            }
        }
    }

    public void matchAndSetLocation(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            _mappedLocation.setValue(null);
            return;
        }
        List<LocationDto> locations = _locations.getValue();
        if (locations == null || locations.isEmpty()) {
            _mappedLocation.setValue(null);
            return;
        }
        
        String query = locationString.toLowerCase().trim();
        LocationDto bestMatch = null;
        
        for (LocationDto dto : locations) {
            String ward = dto.getWard() != null ? dto.getWard().toLowerCase() : "";
            String constituency = dto.getConstituency() != null ? dto.getConstituency().toLowerCase() : "";
            String district = dto.getDistrict() != null ? dto.getDistrict().toLowerCase() : "";
            String displayName = dto.getDisplayName() != null ? dto.getDisplayName().toLowerCase() : "";
            
            if ((!ward.isEmpty() && (query.contains(ward) || ward.contains(query))) ||
                (!constituency.isEmpty() && (query.contains(constituency) || constituency.contains(query))) ||
                (!displayName.isEmpty() && (query.contains(displayName) || displayName.contains(query)))) {
                bestMatch = dto;
                break;
            } else if (!district.isEmpty() && (query.contains(district) || district.contains(query))) {
                if (bestMatch == null) bestMatch = dto;
            }
        }
        
        _mappedLocation.setValue(bestMatch);
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
        _categories.setValue(getDefaultCategories());
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
                this.editingComplaint = complaint;
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

    private List<CategoryDto> getDefaultCategories() {
        List<CategoryDto> defaults = new ArrayList<>();
        defaults.add(new CategoryDto("cat_water", getApplication().getString(R.string.category_water)));
        defaults.add(new CategoryDto("cat_sanitation", getApplication().getString(R.string.category_sanitation)));
        defaults.add(new CategoryDto("cat_roads", getApplication().getString(R.string.category_roads)));
        defaults.add(new CategoryDto("cat_electricity", getApplication().getString(R.string.category_electricity)));
        defaults.add(new CategoryDto("cat_security", getApplication().getString(R.string.category_security)));
        defaults.add(new CategoryDto("cat_sensitive", "Sensitive Issues"));
        defaults.add(new CategoryDto("cat_other", getApplication().getString(R.string.category_other)));
        return defaults;
    }

    private void loadReferenceData() {
        referenceRepository.getCategories(new ReferenceRepository.ReferenceCallback<List<CategoryDto>>() {
            @Override
            public void onSuccess(List<CategoryDto> data) {
                List<CategoryDto> merged = new ArrayList<>();
                if (data != null && !data.isEmpty()) {
                    merged.addAll(data);
                } else {
                    merged.addAll(getDefaultCategories());
                }

                boolean hasSensitive = false;
                for (CategoryDto dto : merged) {
                    if (dto.getName() != null && (dto.getName().equalsIgnoreCase("Sensitive Issues") || dto.getName().toLowerCase().contains("sensitive"))) {
                        hasSensitive = true;
                        break;
                    }
                }
                if (!hasSensitive) {
                    // Insert before "Other" if possible
                    int otherIdx = -1;
                    for (int i = 0; i < merged.size(); i++) {
                        if (merged.get(i).getName() != null && merged.get(i).getName().equalsIgnoreCase("Other")) {
                            otherIdx = i;
                            break;
                        }
                    }
                    if (otherIdx >= 0) {
                        merged.add(otherIdx, new CategoryDto("cat_sensitive", "Sensitive Issues"));
                    } else {
                        merged.add(new CategoryDto("cat_sensitive", "Sensitive Issues"));
                    }
                }
                // Filter out sensitive sub-categories from main grid so only "Sensitive Issues" card appears
                List<CategoryDto> filtered = new ArrayList<>();
                for (CategoryDto dto : merged) {
                    if (dto.getName() == null) continue;
                    String nameLower = dto.getName().toLowerCase().trim();
                    boolean isSubCat = nameLower.contains("gender-based") || 
                                       nameLower.contains("gbv") || 
                                       nameLower.contains("sexual harassment") || 
                                       nameLower.contains("human rights") || 
                                       nameLower.contains("child labour") || 
                                       nameLower.contains("child abuse") || 
                                       nameLower.contains("violence against children");
                    if (!isSubCat) {
                        filtered.add(dto);
                    }
                }
                _categories.setValue(filtered);
            }

            @Override
            public void onError(String message) {
                android.util.Log.w("ComplaintVM", "Failed to fetch categories: " + message);
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

    public static boolean isSensitiveCategory(String categoryName) {
        if (categoryName == null) return false;
        String lower = categoryName.toLowerCase().trim();
        return lower.contains("sensitive") || 
               lower.contains("gender") || 
               lower.contains("gbv") || 
               lower.contains("harassment") || 
               lower.contains("human rights") || 
               lower.contains("child");
    }

    public void submitComplaint(String title, String description, CategoryDto category, String customCategory, LocationDto location, String addressString, double lat, double lon) {
        submitComplaint(title, description, category, customCategory, location, addressString, lat, lon, null, 0, false);
    }

    public void submitComplaint(String title, String description, CategoryDto category, String customCategory, LocationDto location, String addressString, double lat, double lon, String videoPath, int videoDuration) {
        submitComplaint(title, description, category, customCategory, location, addressString, lat, lon, videoPath, videoDuration, false);
    }

    public void submitComplaint(String title, String description, CategoryDto category, String customCategory, LocationDto location, String addressString, double lat, double lon, String videoPath, int videoDuration, boolean isAnonymous) {
        VoiceNoteState vnState = _voiceNoteState.getValue();
        boolean hasVoiceNote = vnState != null && vnState.localFile != null && vnState.localFile.exists();
        boolean hasVideo = videoPath != null && !videoPath.trim().isEmpty();
        
        if (hasVoiceNote && vnState.durationSeconds > 120) {
            _error.setValue("Voice note exceeds 2 minutes max limit");
            return;
        }

        if (hasVideo && videoDuration > 90) {
            _error.setValue("Video exceeds 1 minute 30 seconds max limit");
            return;
        }
        
        String otherCategory = getApplication().getString(R.string.category_other);
        String categoryName = (category != null && !category.getName().equalsIgnoreCase(otherCategory)) 
            ? category.getName() 
            : (customCategory != null && !customCategory.isEmpty() ? customCategory : null);

        String finalLocation = (addressString != null && !addressString.trim().isEmpty()) ? addressString.trim() : 
                              (location != null && location.getDisplayName() != null ? location.getDisplayName().trim() : getApplication().getString(R.string.default_location));

        if (currentClientUuid == null) {
            currentClientUuid = UUID.randomUUID().toString();
        }

        boolean isSensitive = isSensitiveCategory(categoryName);

        // Use the same validation logic as the request model
        com.voiceyanga.citizen.data.remote.dto.ComplaintRequest validationRequest = 
            new com.voiceyanga.citizen.data.remote.dto.ComplaintRequest(
                title,
                description,
                categoryName,
                finalLocation,
                isSensitive ? "CRITICAL" : "MEDIUM",
                currentClientUuid
            );
        validationRequest.isConfidential = isSensitive;
        validationRequest.isAnonymous = isAnonymous;
        
        if (hasVoiceNote) {
            validationRequest.voiceNoteUrl = "temp";
        }

        if (hasVideo) {
            validationRequest.videoUrl = "temp";
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

        // Ensure location object matches finalLocation string, or clear it if it's a mismatch
        if (location != null && finalLocation != null && !finalLocation.trim().isEmpty()) {
            String addrLower = finalLocation.toLowerCase().trim();
            String wardLower = location.getWard() != null ? location.getWard().toLowerCase() : "";
            String constLower = location.getConstituency() != null ? location.getConstituency().toLowerCase() : "";
            String distLower = location.getDistrict() != null ? location.getDistrict().toLowerCase() : "";
            String nameLower = location.getDisplayName() != null ? location.getDisplayName().toLowerCase() : "";
            
            boolean matches = (!wardLower.isEmpty() && (addrLower.contains(wardLower) || wardLower.contains(addrLower))) ||
                              (!constLower.isEmpty() && (addrLower.contains(constLower) || constLower.contains(addrLower))) ||
                              (!distLower.isEmpty() && (addrLower.contains(distLower) || distLower.contains(addrLower))) ||
                              (!nameLower.isEmpty() && (addrLower.contains(nameLower) || nameLower.contains(addrLower)));
                              
            if (!matches) {
                location = null;
            }
        }

        boolean isEditMode = (editingComplaint != null && editingComplaint.getClientUuid().equals(currentClientUuid));

        Complaint complaint;
        if (isEditMode) {
            complaint = editingComplaint;
            complaint.setTitle(title);
            complaint.setDescription(description != null ? description : "");
            complaint.setCategory(categoryName != null ? categoryName : getApplication().getString(R.string.default_category));
            complaint.setLocation(finalLocation);
        } else {
            complaint = new Complaint(
                    currentClientUuid,
                    title,
                    description != null ? description : "",
                    categoryName != null ? categoryName : getApplication().getString(R.string.default_category),
                    finalLocation,
                    "PENDING",
                    System.currentTimeMillis()
            );
        }

        complaint.setLatitude(lat);
        complaint.setLongitude(lon);
        complaint.setHasValidCoordinates(lat != 0.0 && lon != 0.0);
        complaint.setUserId(repository.getUserId());

        if (location != null) {
            complaint.setLocationId(location.getId());
            complaint.setWard(location.getWard());
            complaint.setDistrict(location.getDistrict());
            complaint.setProvince(location.getProvince());
        }
        
        if (category != null && !category.getName().equalsIgnoreCase(otherCategory)) {
            complaint.setCategoryId(category.getId());
        }

        if (hasVoiceNote) {
            complaint.setVoiceNoteLocalPath(vnState.localFile.getAbsolutePath());
            complaint.setVoiceNoteDuration(vnState.durationSeconds);
        }

        if (hasVideo) {
            complaint.setVideoLocalPath(videoPath);
            complaint.setVideoDuration(videoDuration);
        }

        if (isSensitive) {
            complaint.setConfidential(true);
            complaint.setAnonymous(isAnonymous);
            if (isAnonymous) {
                complaint.setAuthorEmail(null);
            }
        }
        
        complaint.setFailureReason(null); // Clear previous errors if any
        
        if (isEditMode) {
            repository.editComplaint(complaint);
            editingComplaint = null;
        } else {
            repository.deleteDraft(); // Clear any existing draft before promoting
            repository.saveComplaint(complaint, _selectedPhotos.getValue(), photoLabels);
        }
        
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