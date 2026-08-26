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
 * [FR-COMP-01] Complaint creation.
 * [FR-COMP-03] Photo attachments.
 * [FR-COMP-04] GPS location integration.
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
    private List<CategoryDto> availableCategories;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private android.net.Uri cameraImageUri;

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
        setupCategorySelection();

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
                    if (fineLocationGranted != null && fineLocationGranted) {
                        getLastLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        getLastLocation();
                    } else {
                        Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        viewModel.addPhoto(uri.toString());
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
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
                        Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> handleBackPress());
    }

    private void handleBackPress() {
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String customCat = binding.etCustomCategory.getText().toString().trim();

        if (!title.isEmpty() || !desc.isEmpty()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Save Draft?")
                    .setMessage("You have unsaved changes. Would you like to save this as a draft?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        viewModel.saveDraft(title, desc, selectedCategory, customCat, currentLocation);
                        finish();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        viewModel.deleteDraft();
                        finish();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            finish();
        }
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoAdapter(new PhotoAdapter.OnRemoveListener() {
            @Override
            public void onRemove(String uri) {
                viewModel.removePhoto(uri);
            }

            @Override
            public void onLabelChanged(String uri, String label) {
                viewModel.updatePhotoLabel(uri, label);
            }
        });
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupObservers() {
        viewModel.getCategories().observe(this, categories -> {
            android.util.Log.d("CreateComplaint", "Categories loaded: " + (categories != null ? categories.size() : "null"));
            if (categories != null) {
                for (CategoryDto c : categories) {
                    android.util.Log.d("CreateComplaint", "Category: " + c.getName() + " [" + c.getId() + "]");
                }
            }
            this.availableCategories = categories;
        });

        viewModel.getLocations().observe(this, locations -> {
            android.util.Log.d("CreateComplaint", "Locations loaded: " + (locations != null ? locations.size() : "null"));
            if (locations != null && !locations.isEmpty()) {
                this.currentLocation = locations.get(0);
            }
        });

        viewModel.getSubmissionSuccess().observe(this, success -> {
            if (success != null && success) {
                com.voiceyanga.citizen.ui.common.SuccessDialogFragment dialog = new com.voiceyanga.citizen.ui.common.SuccessDialogFragment();
                dialog.setOnDismissListener(this::finish);
                dialog.show(getSupportFragmentManager(), "success");
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnSubmit.setEnabled(!isLoading);
            }
        });

        viewModel.getSelectedPhotos().observe(this, photos -> {
            photoAdapter.setPhotos(photos);
        });

        viewModel.getMappedLocation().observe(this, location -> {
            if (location != null) {
                this.currentLocation = location;
                this.detectedAddress = currentLocation.getDisplayName();
                binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedAddress));
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getDraft().observe(this, draft -> {
            if (draft != null) {
                binding.etTitle.setText(draft.getTitle());
                binding.etDescription.setText(draft.getDescription());
                
                String draftCategory = draft.getCategory();
                if (draftCategory != null) {
                    if (draftCategory.equalsIgnoreCase(getString(R.string.category_water))) {
                        selectCategory(draftCategory, binding.cardWater);
                    } else if (draftCategory.equalsIgnoreCase(getString(R.string.category_sanitation))) {
                        selectCategory(draftCategory, binding.cardSanitation);
                    } else if (draftCategory.equalsIgnoreCase(getString(R.string.category_roads))) {
                        selectCategory(draftCategory, binding.cardRoads);
                    } else if (draftCategory.equalsIgnoreCase(getString(R.string.category_electricity))) {
                        selectCategory(draftCategory, binding.cardElectricity);
                    } else if (draftCategory.equalsIgnoreCase(getString(R.string.category_security))) {
                        selectCategory(draftCategory, binding.cardSecurity);
                    } else {
                        // It's a custom category
                        selectCategory(getString(R.string.category_other), binding.cardOther);
                        binding.etCustomCategory.setText(draftCategory);
                    }
                }
            }
        });
    }

    private void setupListeners() {
        binding.etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                suggestCategory(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnSubmit.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String customCategory = binding.etCustomCategory.getText().toString().trim();
            
            viewModel.submitComplaint(title, description, selectedCategory, customCategory, currentLocation, 
                    currentLatitude, currentLongitude);
        });

        binding.btnLocation.setOnClickListener(v -> requestLocationPermissions());

        binding.btnManualLocation.setOnClickListener(v -> showManualLocationDialog());

        binding.btnAddPhotos.setOnClickListener(v -> showPhotoSourceDialog());
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
            Toast.makeText(this, "Locations not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        SearchableLocationDialog dialog = new SearchableLocationDialog(locations, location -> {
            viewModel.setManualLocation(location);
        });
        dialog.show(getSupportFragmentManager(), "search_location");
    }

    private void requestLocationPermissions() {
        if (!isGpsEnabled()) {
            Toast.makeText(this, "Please turn on your GPS location", Toast.LENGTH_LONG).show();
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        binding.pbLocationLoading.setVisibility(View.VISIBLE);
        binding.tvDetectedLocation.setText("Searching for GPS...");
        binding.tvDetectedLocation.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                // Request a fresh location if last known is null
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
        
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        this.detectedAddress = address.getAddressLine(0);
                        binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedAddress));
                        binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                        
                        // Map to API location
                        viewModel.mapToApiLocation(address.getLocality(), address.getSubLocality());
                    }
                    binding.pbLocationLoading.setVisibility(View.GONE);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    String locationName = location.getLatitude() + ", " + location.getLongitude();
                    binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), locationName));
                    binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                    binding.pbLocationLoading.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void setupCategorySelection() {
        binding.cardWater.setOnClickListener(v -> selectCategory(getString(R.string.category_water), binding.cardWater));
        binding.cardSanitation.setOnClickListener(v -> selectCategory(getString(R.string.category_sanitation), binding.cardSanitation));
        binding.cardRoads.setOnClickListener(v -> selectCategory(getString(R.string.category_roads), binding.cardRoads));
        binding.cardElectricity.setOnClickListener(v -> selectCategory(getString(R.string.category_electricity), binding.cardElectricity));
        binding.cardSecurity.setOnClickListener(v -> selectCategory(getString(R.string.category_security), binding.cardSecurity));
        binding.cardOther.setOnClickListener(v -> selectCategory(getString(R.string.category_other), binding.cardOther));
    }

    private void selectCategory(String categoryName, MaterialCardView card) {
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
        
        // Fallback for cases where categories aren't loaded or it's a known static category
        if (selectedCategory == null && !isOther) {
            selectedCategory = new CategoryDto(null, categoryName);
        }
        
        resetCard(binding.cardWater);
        resetCard(binding.cardSanitation);
        resetCard(binding.cardRoads);
        resetCard(binding.cardElectricity);
        resetCard(binding.cardSecurity);
        resetCard(binding.cardOther);
        
        card.setStrokeColor(ContextCompat.getColor(this, R.color.primary_green));
        card.setStrokeWidth(6);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_light));
    }

    private void resetCard(MaterialCardView card) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.neutral_200));
        card.setStrokeWidth(2);
        
        // Use TypedValue to get colorSurface attribute
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        card.setCardBackgroundColor(typedValue.data);
    }

    private void suggestCategory(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("water") || lowerTitle.contains("leak") || lowerTitle.contains("pipe") || lowerTitle.contains("burst")) {
            selectCategory(getString(R.string.category_water), binding.cardWater);
        } else if (lowerTitle.contains("sanitation") || lowerTitle.contains("sewer") || lowerTitle.contains("drainage") || lowerTitle.contains("waste") || lowerTitle.contains("garbage")) {
            selectCategory(getString(R.string.category_sanitation), binding.cardSanitation);
        } else if (lowerTitle.contains("road") || lowerTitle.contains("street") || lowerTitle.contains("pothole") || lowerTitle.contains("bridge")) {
            selectCategory(getString(R.string.category_roads), binding.cardRoads);
        } else if (lowerTitle.contains("electric") || lowerTitle.contains("power") || lowerTitle.contains("light") || lowerTitle.contains("cable") || lowerTitle.contains("transformer")) {
            selectCategory(getString(R.string.category_electricity), binding.cardElectricity);
        } else if (lowerTitle.contains("security") || lowerTitle.contains("crime") || lowerTitle.contains("police") || lowerTitle.contains("danger") || lowerTitle.contains("safety")) {
            selectCategory(getString(R.string.category_security), binding.cardSecurity);
        }
    }
}
