package com.voiceyanga.citizen.feature.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.voiceyanga.citizen.R;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnReset;
    private ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnReset = findViewById(R.id.btnReset);
        pbLoading = findViewById(R.id.pbLoading);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                etEmail.setError(getString(R.string.error_invalid_email));
                return;
            }

            performMockReset(email);
        });
    }

    private void performMockReset(String email) {
        pbLoading.setVisibility(View.VISIBLE);
        btnReset.setEnabled(false);

        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pbLoading.setVisibility(View.GONE);
            btnReset.setEnabled(true);
            Toast.makeText(this, R.string.reset_link_sent, Toast.LENGTH_LONG).show();
            finish();
        }, 1500);
    }
}
