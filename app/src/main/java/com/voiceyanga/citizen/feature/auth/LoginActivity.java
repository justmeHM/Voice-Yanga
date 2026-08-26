package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.utils.BiometricHelper;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.feature.home.HomeActivity;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar pbLoading;

    @Inject
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        pbLoading = findViewById(R.id.pbLoading);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Auto-fill from Registration if applicable
        String prefilledEmail = getIntent().getStringExtra("email");
        String prefilledPassword = getIntent().getStringExtra("password");
        if (prefilledEmail != null) etEmail.setText(prefilledEmail);
        if (prefilledPassword != null) etPassword.setText(prefilledPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (validate(email, password)) {
                viewModel.login(email, password);
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        observeViewModel();
    }

    private boolean validate(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_empty_password));
            return false;
        }
        return true;
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}