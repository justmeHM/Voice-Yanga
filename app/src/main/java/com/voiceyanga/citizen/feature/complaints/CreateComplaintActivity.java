package com.voiceyanga.citizen.feature.complaints;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.utils.HapticHelper;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.databinding.ActivityCreateComplaintBinding;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Screen for creating a new citizen complaint.
 * Refined to a single-screen Smart Composer for enterprise efficiency.
 */
@AndroidEntryPoint
public class CreateComplaintActivity extends AppCompatActivity {

    private ActivityCreateComplaintBinding binding;
    private ComplaintViewModel viewModel;
    private PhotoAdapter photoAdapter;
    private CategoryDto selectedCategory = null;
    private LocationDto currentLocation = null;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String detectedAddress = null;
    private boolean isManualLocation = false;
    private List<CategoryDto> availableCategories;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> voicePermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private android.net.Uri cameraImageUri;
    private boolean isSubmitted = false;

    private final android.os.Handler autoSaveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            triggerAutoSave();
            autoSaveHandler.postDelayed(this, 30000); // 30 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityCreateComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupPermissionLaunchers();
        setupToolbar();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        
        requestLocationPermissions();
        populateSummary();
        
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void setupPermissionLaunchers() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if ((fineLocationGranted != null && fineLocationGranted) || 
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                        getLastLocation();
                    } else {
                        Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        voicePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        HapticHelper.selection(binding.btnRecord);
                        viewModel.startRecording();
                    } else {
                        Toast.makeText(this, R.string.error_microphone_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        HapticHelper.success(this);
                        viewModel.addPhoto(uri.toString());
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        HapticHelper.success(this);
                        viewModel.addPhoto(cameraImageUri.toString());
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> handleBackPress());
    }

    private void triggerAutoSave() {
        if (binding == null || isSubmitted) return;
        
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String customCat = binding.etCustomCategory.getText().toString().trim();

        if (!title.isEmpty() || !desc.isEmpty()) {
            viewModel.saveDraft(title, desc, selectedCategory, customCat, currentLocation, detectedAddress);
        }
    }

    private void handleBackPress() {
        if (binding == null) {
            finish();
            return;
        }

        if (isSubmitted) {
            finish();
            return;
        }
        
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String customCat = binding.etCustomCategory.getText().toString().trim();

        if (!title.isEmpty() || !desc.isEmpty()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_save_draft_title)
                    .setMessage(R.string.dialog_save_draft_message)
                    .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                        viewModel.saveDraft(title, desc, selectedCategory, customCat, currentLocation, detectedAddress);
                        finish();
                    })
                    .setNegativeButton(R.string.btn_discard, (dialog, which) -> {
                        viewModel.deleteDraft();
                        finish();
                    })
                    .setNeutralButton(R.string.btn_cancel, null)
                    .show();
        } else {
            finish();
        }
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoAdapter(new PhotoAdapter.OnRemoveListener() {
            @Override
            public void onRemove(String uri) {
                if (uri != null) viewModel.removePhoto(uri);
            }

            @Override
            public void onPhotoClick(String uri) {
                showPhotoPreview(uri);
            }

            @Override
            public void onLabelChanged(String uri, String label) {
                if (uri != null) viewModel.updatePhotoLabel(uri, label);
            }
        });
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupObservers() {
        viewModel.getCategories().observe(this, categories -> {
            if (categories != null) {
                this.availableCategories = categories;
            }
        });

        viewModel.getLocations().observe(this, locations -> {
            if (locations != null && !locations.isEmpty()) {
                this.currentLocation = locations.get(0);
            }
        });

        viewModel.getSubmissionSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                com.voiceyanga.citizen.ui.common.SuccessDialogFragment dialog = new com.voiceyanga.citizen.ui.common.SuccessDialogFragment();
                dialog.setOnDismissListener(this::finish);
                dialog.show(getSupportFragmentManager(), "success");
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                binding.btnSubmit.setEnabled(true);
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null && binding != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnSubmit.setEnabled(!isLoading);
            }
        });

        viewModel.getSelectedPhotos().observe(this, photos -> {
            if (photos != null) {
                photoAdapter.setPhotos(photos);
                populateSummary();
            }
        });

        viewModel.getMappedLocation().observe(this, location -> {
            if (location != null && binding != null) {
                this.currentLocation = location;
                this.detectedAddress = currentLocation.getDisplayName();
                binding.tvDetectedLocation.setText(detectedAddress);
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                binding.pbLocationLoading.setVisibility(View.GONE);
                populateSummary();
            }
        });

        viewModel.getDraft().observe(this, draft -> {
            if (draft != null && binding != null) {
                binding.etTitle.setText(draft.getTitle());
                binding.etDescription.setText(draft.getDescription());
                
                String draftCategory = draft.getCategory();
                if (draftCategory != null) {
                    selectCategory(draftCategory);
                }
                populateSummary();
            }
        });

        viewModel.getVoiceNoteState().observe(this, state -> {
            if (state == null || binding == null) return;
            
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show();
            }
            
            binding.cardVoiceNote.setVisibility(state.isRecording || state.localFile != null ? View.VISIBLE : View.GONE);
            binding.llRecordControls.setVisibility(state.isRecording ? View.VISIBLE : View.GONE);
            binding.llPlaybackControls.setVisibility(state.localFile != null ? View.VISIBLE : View.GONE);
            
            if (state.isRecording) {
                int minutes = state.elapsedSeconds / 60;
                int seconds = state.elapsedSeconds % 60;
                binding.tvRecordingTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                binding.visualizer.addAmplitude(state.amplitude);
            } else if (state.localFile != null) {
                binding.visualizer.clear();
                binding.btnPlay.setVisibility(state.isPlaying ? View.GONE : View.VISIBLE);
                binding.btnPause.setVisibility(state.isPlaying ? View.VISIBLE : View.GONE);
                
                int duration = state.durationSeconds;
                int minutes = duration / 60;
                int seconds = duration % 60;
                binding.tvVoiceDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                populateSummary();
            }
        });
    }

    private void setupListeners() {
        binding.etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) suggestCategory(s.toString());
                populateSummary();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { populateSummary(); }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnSubmit.setOnClickListener(v -> {
            if (isSubmitted) return;
            
            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String customCategory = binding.etCustomCategory.getText().toString().trim();
            
            binding.btnSubmit.setEnabled(false);
            viewModel.submitComplaint(title, description, selectedCategory, customCategory, currentLocation, 
                    detectedAddress, currentLatitude, currentLongitude);
            isSubmitted = true;
        });

        binding.btnManualLocation.setOnClickListener(v -> showManualLocationDialog());
        binding.btnAddPhotos.setOnClickListener(v -> showPhotoSourceDialog());
        binding.cardCategorySelector.setOnClickListener(v -> showCategoryDialog());
        binding.cardReviewSummary.setOnClickListener(v -> showFullReviewDialog());

        binding.btnRecord.setOnClickListener(v -> checkVoicePermission());
        binding.btnStop.setOnClickListener(v -> {
            HapticHelper.selection(v);
            viewModel.stopRecording();
        });
        binding.btnPlay.setOnClickListener(v -> viewModel.playRecording());
        binding.btnPause.setOnClickListener(v -> viewModel.pauseRecording());
        binding.btnDeleteVoice.setOnClickListener(v -> {
            HapticHelper.longPress(v);
            viewModel.deleteRecording();
            populateSummary();
        });
    }

    private void checkVoicePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            HapticHelper.selection(binding.btnRecord);
            viewModel.startRecording();
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void showPhotoSourceDialog() {
        String[] options = {getString(R.string.option_take_photo), getString(R.string.option_choose_gallery)};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.photos_label)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        File imageFolder = new File(getCacheDir(), "images");
        if (!imageFolder.exists()) {
            boolean ignored = imageFolder.mkdirs();
        }
        File photoFile = new File(imageFolder, "cam_" + System.currentTimeMillis() + ".jpg");
        
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this, 
                getPackageName() + ".provider", photoFile);
        
        cameraLauncher.launch(cameraImageUri);
    }

    private void showManualLocationDialog() {
        List<LocationDto> locations = viewModel.getLocations().getValue();
        if (locations == null || locations.isEmpty()) {
            android.widget.EditText etLocation = new android.widget.EditText(this);
            etLocation.setHint("e.g. Matero, Ward 24");
            etLocation.setPadding(48, 48, 48, 48);
            
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.title_select_location)
                    .setView(etLocation)
                    .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                        String manualName = etLocation.getText().toString().trim();
                        if (!manualName.isEmpty()) {
                            this.detectedAddress = manualName;
                            this.isManualLocation = true;
                            binding.tvDetectedLocation.setText(detectedAddress);
                            binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                            populateSummary();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
            return;
        }

        SearchableLocationDialog dialog = new SearchableLocationDialog(locations, location -> {
            isManualLocation = true;
            viewModel.setManualLocation(location);
        });
        dialog.show(getSupportFragmentManager(), "search_location");
    }

    private void requestLocationPermissions() {
        if (!isGpsEnabled()) {
            Toast.makeText(this, R.string.error_gps_off, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        binding.pbLocationLoading.setVisibility(View.VISIBLE);
        binding.tvDetectedLocation.setText(R.string.status_searching_gps);
        binding.tvDetectedLocation.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> {
            requestFreshLocation();
        });
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.pbLocationLoading.setVisibility(View.GONE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        updateLocationUI(location);
                    } else {
                        binding.pbLocationLoading.setVisibility(View.GONE);
                        Toast.makeText(CreateComplaintActivity.this, R.string.error_location_not_found, Toast.LENGTH_SHORT).show();
                        binding.tvDetectedLocation.setVisibility(View.GONE);
                    }
                }
            };
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());
    }

    private void updateLocationUI(Location location) {
        this.currentLatitude = location.getLatitude();
        this.currentLongitude = location.getLongitude();
        this.isManualLocation = false;
        
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        String neighborhood = address.getSubLocality();
                        String city = address.getLocality();
                        String province = address.getAdminArea();
                        
                        if (neighborhood != null && !neighborhood.isEmpty() && !neighborhood.contains("+")) {
                            sb.append(neighborhood);
                            if (city != null) sb.append(", ").append(city);
                        } else if (city != null && !city.isEmpty()) {
                            sb.append(city);
                            if (province != null) sb.append(", ").append(province);
                        } else {
                            String line = address.getAddressLine(0);
                            if (line != null && line.contains("+")) {
                                String clean = line.replaceAll("^[A-Z0-9]{4}\\+[A-Z0-9]{2,3}\\s*", "");
                                sb.append(clean.isEmpty() ? "Unknown Location" : clean);
                            } else {
                                sb.append(line != null ? line : "Unknown Location");
                            }
                        }
                        
                        this.detectedAddress = sb.toString();
                        binding.tvDetectedLocation.setText(detectedAddress);
                        binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                        viewModel.mapToApiLocation(address.getLocality(), address.getSubLocality());
                    }
                    binding.pbLocationLoading.setVisibility(View.GONE);
                    populateSummary();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    String coords = String.format(Locale.getDefault(), "%.5f, %.5f", location.getLatitude(), location.getLongitude());
                    binding.tvDetectedLocation.setText(coords);
                    binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                    binding.pbLocationLoading.setVisibility(View.GONE);
                    populateSummary();
                });
            }
        }).start();
    }

    private void showCategoryDialog() {
        String[] items = {
            getString(R.string.category_water),
            getString(R.string.category_sanitation),
            getString(R.string.category_roads),
            getString(R.string.category_electricity),
            getString(R.string.category_security),
            getString(R.string.category_other)
        };
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_label)
                .setItems(items, (dialog, which) -> {
                    selectCategory(items[which]);
                })
                .show();
    }

    private void selectCategory(String categoryName) {
        boolean isOther = categoryName.equalsIgnoreCase(getString(R.string.category_other));
        binding.tilCustomCategory.setVisibility(isOther ? View.VISIBLE : View.GONE);
        
        selectedCategory = null;
        if (availableCategories != null) {
            for (CategoryDto dto : availableCategories) {
                if (dto.getName().equalsIgnoreCase(categoryName) || 
                    dto.getName().toLowerCase().contains(categoryName.toLowerCase())) {
                    selectedCategory = dto;
                    break;
                }
            }
        }
        
        if (selectedCategory == null && !isOther) {
            selectedCategory = new CategoryDto(null, categoryName);
        }
        
        binding.tvCategoryName.setText(categoryName);
        
        int iconRes = R.drawable.about;
        if (categoryName.equalsIgnoreCase(getString(R.string.category_water))) iconRes = R.drawable.water;
        else if (categoryName.equalsIgnoreCase(getString(R.string.category_sanitation))) iconRes = R.drawable.sanitary;
        else if (categoryName.equalsIgnoreCase(getString(R.string.category_roads))) iconRes = R.drawable.road;
        else if (categoryName.equalsIgnoreCase(getString(R.string.category_electricity))) iconRes = R.drawable.electricity;
        else if (categoryName.equalsIgnoreCase(getString(R.string.category_security))) iconRes = R.drawable.security;
        
        binding.ivCategoryIcon.setImageResource(iconRes);
        populateSummary();
    }

    private void suggestCategory(String title) {
        String lowerTitle = title.toLowerCase();
        binding.badgeSuggestion.setVisibility(View.GONE);

        String suggested = null;
        if (lowerTitle.contains("water") || lowerTitle.contains("leak") || lowerTitle.contains("pipe") || lowerTitle.contains("burst")) {
            suggested = getString(R.string.category_water);
        } else if (lowerTitle.contains("sanitation") || lowerTitle.contains("sewer") || lowerTitle.contains("drainage") || lowerTitle.contains("waste") || lowerTitle.contains("garbage")) {
            suggested = getString(R.string.category_sanitation);
        } else if (lowerTitle.contains("road") || lowerTitle.contains("street") || lowerTitle.contains("pothole") || lowerTitle.contains("bridge")) {
            suggested = getString(R.string.category_roads);
        } else if (lowerTitle.contains("electric") || lowerTitle.contains("power") || lowerTitle.contains("light") || lowerTitle.contains("cable") || lowerTitle.contains("transformer")) {
            suggested = getString(R.string.category_electricity);
        } else if (lowerTitle.contains("security") || lowerTitle.contains("crime") || lowerTitle.contains("police") || lowerTitle.contains("danger") || lowerTitle.contains("safety")) {
            suggested = getString(R.string.category_security);
        }
        
        if (suggested != null) {
            selectCategory(suggested);
            binding.badgeSuggestion.setVisibility(View.VISIBLE);
        }
    }

    private void populateSummary() {
        if (binding == null) return;
        
        String title = binding.etTitle.getText().toString().trim();
        binding.tvSummaryTitle.setText(title.isEmpty() ? "---" : title);
        binding.tvSummaryCategory.setText(String.format(getString(R.string.label_category_format), 
                selectedCategory != null ? selectedCategory.getName() : getString(R.string.default_category)));
        
        List<String> photos = viewModel.getSelectedPhotos().getValue();
        int photoCount = photos != null ? photos.size() : 0;
        ComplaintViewModel.VoiceNoteState vnState = viewModel.getVoiceNoteState().getValue();
        boolean hasVoice = vnState != null && vnState.localFile != null;
        
        String mediaText = String.format(Locale.getDefault(), "Evidence: %d Photos • %s", 
                photoCount, hasVoice ? getString(R.string.label_voice_note_present) : getString(R.string.label_voice_note_absent));
        binding.tvSummaryMedia.setText(mediaText);
        
        binding.tvSummaryLocation.setText(String.format(getString(R.string.label_location_format), 
                detectedAddress != null ? detectedAddress : getString(R.string.status_searching_gps)));
    }

    private void showPhotoPreview(String uri) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.shimmer_complaint_detail); // Using a placeholder or creating a simple layout
        
        // Better: create a quick layout for full screen image
        android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(android.graphics.Color.BLACK);
        
        com.bumptech.glide.Glide.with(this).load(uri).into(iv);
        
        dialog.setContentView(iv);
        iv.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showFullReviewDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_report_review_bottom_sheet, null);
        dialog.setContentView(view);
        
        android.widget.TextView tvTitle = view.findViewById(R.id.tvReviewTitle);
        android.widget.TextView tvCategory = view.findViewById(R.id.tvReviewCategory);
        android.widget.TextView tvDescription = view.findViewById(R.id.tvReviewDescription);
        android.widget.TextView tvLocation = view.findViewById(R.id.tvReviewLocation);
        
        tvTitle.setText(binding.etTitle.getText().toString().trim());
        tvCategory.setText(selectedCategory != null ? selectedCategory.getName().toUpperCase() : "GENERAL");
        tvDescription.setText(binding.etDescription.getText().toString().trim());
        tvLocation.setText(detectedAddress != null ? detectedAddress : "No location detected");
        
        // Photos
        List<String> photos = viewModel.getSelectedPhotos().getValue();
        if (photos != null && !photos.isEmpty()) {
            androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rvReviewPhotos);
            PhotoAdapter previewAdapter = new PhotoAdapter(null); // No removal in preview
            previewAdapter.setPhotos(photos);
            rv.setAdapter(previewAdapter);
        } else {
            view.findViewById(R.id.tvMediaLabel).setVisibility(View.GONE);
            view.findViewById(R.id.rvReviewPhotos).setVisibility(View.GONE);
        }

        // Voice Note
        ComplaintViewModel.VoiceNoteState vnState = viewModel.getVoiceNoteState().getValue();
        if (vnState != null && vnState.localFile != null) {
            view.findViewById(R.id.cardReviewVoice).setVisibility(View.VISIBLE);
            android.widget.TextView tvDuration = view.findViewById(R.id.tvReviewVoiceDuration);
            int duration = vnState.durationSeconds;
            tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", duration / 60, duration % 60));
        }
        
        view.findViewById(R.id.btnCloseReview).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        triggerAutoSave();
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoSaveHandler.postDelayed(autoSaveRunnable, 30000);
    }
}
