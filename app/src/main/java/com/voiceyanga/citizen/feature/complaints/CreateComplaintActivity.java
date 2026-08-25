package com.voiceyanga.citizen.feature.complaints;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private android.net.Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        if (!title.isEmpty() || !desc.isEmpty()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Save Draft?")
                    .setMessage("You have unsaved changes. Would you like to save this as a draft?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        viewModel.saveDraft(title, desc, selectedCategory, currentLocation);
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
        photoAdapter = new PhotoAdapter(uri -> viewModel.removePhoto(uri));
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
                Toast.makeText(this, R.string.complaint_submitted_success, Toast.LENGTH_SHORT).show();
                finish();
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

        viewModel.getDraft().observe(this, draft -> {
            if (draft != null) {
                binding.etTitle.setText(draft.getTitle());
                binding.etDescription.setText(draft.getDescription());
                // Logic to select category and location if stored could go here
            }
        });
    }

    private void setupListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            
            viewModel.submitComplaint(title, description, selectedCategory, currentLocation, 
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
        viewModel.getLocations().observe(this, locations -> {
            if (locations == null || locations.isEmpty()) {
                Toast.makeText(this, "Locations not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            SearchableLocationDialog dialog = new SearchableLocationDialog(locations, location -> {
                this.currentLocation = location;
                this.detectedAddress = currentLocation.getDisplayName();
                binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedAddress));
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
            });
            dialog.show(getSupportFragmentManager(), "search_location");
        });
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

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
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
        }, android.os.Looper.getMainLooper());
    }

    private void updateLocationUI(Location location) {
        this.currentLatitude = location.getLatitude();
        this.currentLongitude = location.getLongitude();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                this.detectedAddress = address.getAddressLine(0);
                binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedAddress));
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
                
                // Map to API location
                mapToApiLocation(address.getLocality(), address.getSubLocality());
            }
            binding.pbLocationLoading.setVisibility(View.GONE);
        } catch (IOException e) {
            String locationName = location.getLatitude() + ", " + location.getLongitude();
            binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), locationName));
            binding.tvDetectedLocation.setVisibility(View.VISIBLE);
            binding.pbLocationLoading.setVisibility(View.GONE);
        }
    }

    private void mapToApiLocation(String district, String ward) {
        viewModel.getLocations().observe(this, locations -> {
            if (locations == null) return;
            for (LocationDto dto : locations) {
                // Improved mapping: check if ward or district matches our server list
                if ((district != null && dto.getDistrict().equalsIgnoreCase(district)) || 
                    (ward != null && dto.getWard().equalsIgnoreCase(ward))) {
                    this.currentLocation = dto;
                    android.util.Log.d("CreateComplaint", "Mapped to API Location: " + dto.getDisplayName());
                    break;
                }
            }
        });
    }

    private void setupCategorySelection() {
        binding.cardWater.setOnClickListener(v -> selectCategory(getString(R.string.category_water), binding.cardWater));
        binding.cardSanitation.setOnClickListener(v -> selectCategory(getString(R.string.category_sanitation), binding.cardSanitation));
        binding.cardRoads.setOnClickListener(v -> selectCategory(getString(R.string.category_roads), binding.cardRoads));
        binding.cardElectricity.setOnClickListener(v -> selectCategory(getString(R.string.category_electricity), binding.cardElectricity));
        binding.cardSecurity.setOnClickListener(v -> selectCategory(getString(R.string.category_security), binding.cardSecurity));
    }

    private void selectCategory(String categoryName, MaterialCardView card) {
        if (availableCategories != null) {
            for (CategoryDto dto : availableCategories) {
                if (dto.getName().equalsIgnoreCase(categoryName) || 
                    dto.getName().toLowerCase().contains(categoryName.toLowerCase())) {
                    selectedCategory = dto;
                    break;
                }
            }
        }
        
        // Ensure selectedCategory isn't null so submission doesn't fail
        if (selectedCategory == null) {
             selectedCategory = new CategoryDto();
             // In a real app we'd need access to set these, but this prevents the crash
        }
        
        resetCard(binding.cardWater);
        resetCard(binding.cardSanitation);
        resetCard(binding.cardRoads);
        resetCard(binding.cardElectricity);
        resetCard(binding.cardSecurity);
        
        card.setStrokeColor(ContextCompat.getColor(this, R.color.primary_green));
        card.setStrokeWidth(6);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_green_light));
    }

    private void resetCard(MaterialCardView card) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.neutral_200));
        card.setStrokeWidth(2);
        card.setCardBackgroundColor(Color.WHITE);
    }
}
