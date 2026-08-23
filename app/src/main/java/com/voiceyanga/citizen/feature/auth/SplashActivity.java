package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.feature.home.HomeActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SplashViewModel viewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        viewModel.getIsServerReachable().observe(this, reachable -> {
            if (reachable) {
                Log.d(TAG, "API Server is reachable");
            } else {
                Log.e(TAG, "API Server is UNREACHABLE. Check local server and BASE_URL.");
                Toast.makeText(this, "Backend Unreachable (Iteration 1 Testing)", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoggedIn().observe(this, loggedIn -> {
            Intent nextIntent;
            if (loggedIn) {
                nextIntent = new Intent(SplashActivity.this, HomeActivity.class);
                // Propagate deep link extras
                if (getIntent().getExtras() != null) {
                    nextIntent.putExtras(getIntent().getExtras());
                }
            } else {
                nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(nextIntent);
            finish();
        });

        viewModel.checkServerHealth();
        viewModel.checkSession();
    }
}