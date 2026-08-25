package com.voiceyanga.citizen.feature.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityProfileBinding;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupToolbar();
        setupObservers();
        setupListeners();
        setupBiometricSwitch();
    }

    private void setupBiometricSwitch() {
        binding.switchBiometric.setChecked(sessionManager.isBiometricEnabled());
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setBiometricEnabled(isChecked);
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupObservers() {
        viewModel.getUserProfile().observe(this, user -> {
            if (user != null) {
                binding.etFirstName.setText(user.getFirstName());
                binding.etLastName.setText(user.getLastName());
                binding.etEmail.setText(user.getEmail());
                binding.etPhone.setText(user.getPhone());
            }
        });

        viewModel.getUpdateSuccess().observe(this, success -> {
            if (success != null && success) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnUpdateProfile.setOnClickListener(v -> {
            String firstName = binding.etFirstName.getText().toString().trim();
            String lastName = binding.etLastName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            viewModel.updateProfile(firstName, lastName, phone);
        });
    }
}
