package com.voiceyanga.citizen.feature.profile;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityProfileBinding;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import android.content.Intent;
import com.voiceyanga.citizen.feature.auth.SplashActivity;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_account) {
            showDeleteConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(R.string.dialog_delete_account_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    viewModel.deleteAccount();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
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

        viewModel.getDeleteSuccess().observe(this, success -> {
            if (success != null && success) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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
