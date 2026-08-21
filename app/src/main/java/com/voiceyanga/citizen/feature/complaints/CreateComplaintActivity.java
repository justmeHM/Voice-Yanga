package com.voiceyanga.citizen.feature.complaints;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.Chip;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityCreateComplaintBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateComplaintActivity extends AppCompatActivity {

    private ActivityCreateComplaintBinding binding;
    private ComplaintViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ComplaintViewModel.class);

        setupToolbar();
        setupObservers();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
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
            String category = getSelectedCategory();
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

    private String getSelectedCategory() {
        int checkedChipId = binding.cgCategory.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = findViewById(checkedChipId);
            return chip.getText().toString();
        }
        return null;
    }
}