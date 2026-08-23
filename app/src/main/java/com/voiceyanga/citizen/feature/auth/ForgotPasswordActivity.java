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
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnReset;
    private ProgressBar pbLoading;

    @Inject
    AuthRepository authRepository;

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

            performReset(email);
        });
    }

    private void performReset(String email) {
        pbLoading.setVisibility(View.VISIBLE);
        btnReset.setEnabled(false);

        authRepository.resetPassword(email, success -> {
            pbLoading.setVisibility(View.GONE);
            btnReset.setEnabled(true);
            if (success) {
                Toast.makeText(this, R.string.reset_link_sent, Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to send reset link", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
