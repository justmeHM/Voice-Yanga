package com.voiceyanga.citizen.ui.common;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityAboutBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Static About Activity showing mission statement and version info.
 */
@AndroidEntryPoint
public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        displayVersionInfo();
        setupSocialLinks();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSocialLinks() {
        binding.ivWebsite.setOnClickListener(v -> openUrl("https://voiceyanga.com"));
        binding.ivFacebook.setOnClickListener(v -> openUrl("https://facebook.com/voiceyanga"));
        binding.ivTwitter.setOnClickListener(v -> openUrl("https://twitter.com/voiceyanga"));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayVersionInfo() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            binding.tvVersion.setText(String.format(getString(R.string.about_version_label), version));
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvVersion.setText(String.format(getString(R.string.about_version_label), "1.0.0"));
        }
    }
}
