package com.voiceyanga.citizen.feature.profile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityProfileBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Profile Activity currently serving as a placeholder.
 */
@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupPlaceholder();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPlaceholder() {
        binding.tvProfileTitle.setText(String.format(getString(R.string.coming_soon_format), getString(R.string.menu_profile)));
    }
}
