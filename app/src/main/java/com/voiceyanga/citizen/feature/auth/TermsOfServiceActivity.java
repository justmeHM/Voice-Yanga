package com.voiceyanga.citizen.feature.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.databinding.ActivityTermsOfServiceBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity to display the Terms of Service.
 */
@AndroidEntryPoint
public class TermsOfServiceActivity extends AppCompatActivity {

    private ActivityTermsOfServiceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsOfServiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
}
