package com.voiceyanga.citizen.feature.complaints;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityCreateComplaintBinding;
import java.io.IOException;
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
    private String selectedCategory = null;
    private String detectedLocation = null;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

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
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoAdapter(uri -> viewModel.removePhoto(uri));
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupObservers() {
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
    }

    private void setupListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String category = selectedCategory;
            String location = detectedLocation != null ? detectedLocation : getString(R.string.location_placeholder);

            viewModel.submitComplaint(title, description, category, location);
        });

        binding.btnLocation.setOnClickListener(v -> requestLocationPermissions());

        binding.btnAddPhotos.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                Toast.makeText(this, R.string.error_location_not_found, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                detectedLocation = address.getAddressLine(0);
                binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedLocation));
                binding.tvDetectedLocation.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            detectedLocation = location.getLatitude() + ", " + location.getLongitude();
            binding.tvDetectedLocation.setText(String.format(getString(R.string.location_not_detected), detectedLocation));
            binding.tvDetectedLocation.setVisibility(View.VISIBLE);
        }
    }

    private void setupCategorySelection() {
        binding.cardWater.setOnClickListener(v -> selectCategory(getString(R.string.category_water), binding.cardWater));
        binding.cardSanitation.setOnClickListener(v -> selectCategory(getString(R.string.category_sanitation), binding.cardSanitation));
        binding.cardRoads.setOnClickListener(v -> selectCategory(getString(R.string.category_roads), binding.cardRoads));
        binding.cardElectricity.setOnClickListener(v -> selectCategory(getString(R.string.category_electricity), binding.cardElectricity));
        binding.cardSecurity.setOnClickListener(v -> selectCategory(getString(R.string.category_security), binding.cardSecurity));
    }

    private void selectCategory(String category, MaterialCardView card) {
        selectedCategory = category;
        
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
