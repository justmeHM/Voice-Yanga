package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.feature.home.HomeActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SplashViewModel viewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        viewModel.getIsLoggedIn().observe(this, loggedIn -> {
            Intent intent = loggedIn
                    ? new Intent(SplashActivity.this, HomeActivity.class)
                    : new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        viewModel.checkSession();
    }
}