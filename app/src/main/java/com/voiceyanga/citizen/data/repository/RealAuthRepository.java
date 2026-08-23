package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.AuthResponse;
import com.voiceyanga.citizen.data.remote.dto.LoginRequest;
import com.voiceyanga.citizen.data.remote.dto.RegisterRequest;
import com.voiceyanga.citizen.domain.repository.AuthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RealAuthRepository implements AuthRepository {

    private final ApiService apiService;
    private final SessionManager sessionManager;

    @Inject
    public RealAuthRepository(ApiService apiService, SessionManager sessionManager) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void login(String email, String password, LoginCallback callback) {
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    sessionManager.saveTokens(authResponse.getAccessToken(), authResponse.getRefreshToken());
                    
                    if (authResponse.getUser() != null) {
                        sessionManager.saveUser(
                                authResponse.getUser().getFullName(),
                                authResponse.getUser().getEmail(),
                                authResponse.getUser().getPhone(),
                                authResponse.getUser().getRole()
                        );
                    }
                    callback.onSuccess();
                } else {
                    callback.onError("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void register(String firstName, String lastName, String phone, String email, String password, LoginCallback callback) {
        RegisterRequest request = new RegisterRequest(firstName, lastName, phone, email, password);
        apiService.register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Save user data locally so greeting works
                    sessionManager.saveUser(firstName + " " + lastName, email, phone, "CITIZEN");
                    callback.onSuccess();
                } else if (response.code() == 409) {
                    callback.onError("Account already exists with this email or phone.");
                } else {
                    callback.onError("Registration failed (Code " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void logout(LogoutCallback callback) {
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken != null) {
            Map<String, String> body = new HashMap<>();
            body.put("refreshToken", refreshToken);
            apiService.logout(body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    sessionManager.clearSession();
                    callback.onResult(true);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    sessionManager.clearSession();
                    callback.onResult(true); // Still clear session locally
                }
            });
        } else {
            sessionManager.clearSession();
            callback.onResult(true);
        }
    }

    @Override
    public void resetPassword(String email, LogoutCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        apiService.requestPasswordReset(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                callback.onResult(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(false);
            }
        });
    }
}
