package com.voiceyanga.citizen.ui.common;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.databinding.ActivitySupportBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Static Support Activity providing contact information.
 */
@AndroidEntryPoint
public class SupportActivity extends AppCompatActivity {

    private ActivitySupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
}
