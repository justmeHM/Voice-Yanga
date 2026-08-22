package com.voiceyanga.citizen.feature.complaints;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.card.MaterialCardView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityCreateComplaintBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateComplaintActivity extends AppCompatActivity {

    private ActivityCreateComplaintBinding binding;
    private ComplaintViewModel viewModel;
    private String selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);

        setupToolbar();
        setupObservers();
        setupListeners();
        setupCategorySelection();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
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
    }

    private void setupListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            String description = binding.etDescription.getText().toString().trim();
            String category = selectedCategory;
            String location = "Matero, Lusaka"; // Placeholder for now

            viewModel.submitComplaint(title, description, category, location);
        });

        binding.btnLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Location detected: Matero, Lusaka", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddPhotos.setOnClickListener(v -> {
            Toast.makeText(this, "Photo selection coming soon", Toast.LENGTH_SHORT).show();
        });
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
        card.setCardBackgroundColor(Color.parseColor("#F0FDF4")); // Very light green
    }

    private void resetCard(MaterialCardView card) {
        card.setStrokeColor(Color.parseColor("#E5E7EB"));
        card.setStrokeWidth(2);
        card.setCardBackgroundColor(Color.WHITE);
    }
}
