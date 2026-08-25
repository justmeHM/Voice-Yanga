package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.utils.BiometricHelper;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.feature.home.HomeActivity;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Inject
    SessionManager sessionManager;

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
            if (loggedIn) {
                if (sessionManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(this)) {
                    BiometricHelper.showPrompt(this, new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onAuthenticated() {
                            startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            // On failure/cancel, go to Login for security
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                        }
                    });
                } else {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                    finish();
                }
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        });

        viewModel.checkServerHealth();
        viewModel.checkSession();
    }
}