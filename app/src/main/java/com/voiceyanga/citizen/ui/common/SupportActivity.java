package com.voiceyanga.citizen.ui.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.R;
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
        setupContactActions();
        setupFaqLogic();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupContactActions() {
        binding.cardEmail.setOnClickListener(v -> sendEmail());
        binding.cardPhone.setOnClickListener(v -> makeCall());
        binding.btnJoinWhatsApp.setOnClickListener(v -> openWhatsApp());
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + getString(R.string.support_email)));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Voice Yanga Support Request");
        try {
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeCall() {
        // Just picking the first number if multiple
        String phones = getString(R.string.support_phone);
        String firstPhone = phones.contains(",") ? phones.split(",")[0] : phones;
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + firstPhone));
        startActivity(intent);
    }

    private void openWhatsApp() {
        String url = getString(R.string.community_whatsapp_url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void setupFaqLogic() {
        binding.layoutFaq1.setOnClickListener(v -> toggleFaq(binding.tvFaqAnswer1, binding.ivFaqArrow1));
        binding.layoutFaq2.setOnClickListener(v -> toggleFaq(binding.tvFaqAnswer2, binding.ivFaqArrow2));
        binding.layoutFaq3.setOnClickListener(v -> toggleFaq(binding.tvFaqAnswer3, binding.ivFaqArrow3));
    }

    private void toggleFaq(View answerView, View arrowView) {
        boolean isVisible = answerView.getVisibility() == View.VISIBLE;
        answerView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        arrowView.setRotation(isVisible ? 0 : 180);
    }
}
