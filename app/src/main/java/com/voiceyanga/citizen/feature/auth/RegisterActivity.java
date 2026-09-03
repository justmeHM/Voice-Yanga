package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.feature.home.HomeActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TextInputEditText etFirstName, etLastName, etPhone, etEmail, etPassword;
    private MaterialButton btnRegister;
    private ProgressBar pbLoading;
    private TextView tvLogin, tvTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        pbLoading = findViewById(R.id.pbLoading);
        tvLogin = findViewById(R.id.tvLogin);
        tvTerms = findViewById(R.id.tvTerms);

        setupTermsLink();

        btnRegister.setOnClickListener(v -> {
            String fName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
            String lName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (validate(fName, lName, phone, email, password)) {
                viewModel.register(fName, lName, phone, email, password);
            }
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        observeViewModel();
    }

    private void setupTermsLink() {
        String fullText = getString(R.string.register_terms_consent);
        String tos = getString(R.string.terms_of_service_title);
        
        SpannableString ss = new SpannableString(fullText);
        
        int start = fullText.indexOf(tos);
        if (start != -1) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View textView) {
                    String url = getString(R.string.terms_of_service_url);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(Color.BLUE);
                }
            };
            ss.setSpan(clickableSpan, start, start + tos.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvTerms.setText(ss);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private boolean validate(String fName, String lName, String phone, String email, String password) {
        if (fName.isEmpty()) {
            etFirstName.setError(getString(R.string.error_invalid_first_name));
            return false;
        }
        if (lName.isEmpty()) {
            etLastName.setError(getString(R.string.error_invalid_last_name));
            return false;
        }
        if (phone.isEmpty()) {
            etPhone.setError(getString(R.string.error_invalid_phone));
            return false;
        }
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        if (password.length() < 8) {
            etPassword.setError(getString(R.string.error_invalid_password));
            return false;
        }
        return true;
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("email", etEmail.getText() != null ? etEmail.getText().toString().trim() : "");
                intent.putExtra("password", etPassword.getText() != null ? etPassword.getText().toString().trim() : "");
                startActivity(intent);
                finish();
            }
        });
    }
}