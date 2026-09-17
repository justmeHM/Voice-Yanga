package com.voiceyanga.citizen.data.repository;

import com.google.firebase.messaging.FirebaseMessaging;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.database.AppDatabase;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.AuthResponse;
import com.voiceyanga.citizen.data.remote.dto.LoginRequest;
import com.voiceyanga.citizen.data.remote.dto.RefreshRequest;
import com.voiceyanga.citizen.data.remote.dto.RegisterRequest;
import com.voiceyanga.citizen.domain.repository.AuthRepository;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class RealAuthRepository implements AuthRepository {

    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase database;
    private final UserRepository userRepository;

    @Inject
    public RealAuthRepository(ApiService apiService, SessionManager sessionManager, AppDatabase database, UserRepository userRepository) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.database = database;
        this.userRepository = userRepository;
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
                    if (authResponse != null && authResponse.getToken() != null && authResponse.getRefreshToken() != null && authResponse.getUser() != null) {
                        android.util.Log.d("AuthRepo", "Login success. Token present: " + (authResponse.getToken() != null));
                        
                        // Save tokens immediately so following requests (like FCM sync) can use them
                        sessionManager.saveTokens(authResponse.getToken(), authResponse.getRefreshToken());
                        
                        if (authResponse.getUser() != null) {
                            android.util.Log.d("AuthRepo", "Saving user: " + authResponse.getUser().getId());
                            sessionManager.saveUser(
                                    authResponse.getUser().getId(),
                                    authResponse.getUser().getFullName(),
                                    authResponse.getUser().getEmail(),
                                    authResponse.getUser().getPhone(),
                                    authResponse.getUser().getRole()
                            );
                        }

                        // Clear database in background
                        new Thread(() -> {
                            try {
                                android.util.Log.d("AuthRepo", "Clearing all tables...");
                                database.clearAllTables();
                                android.util.Log.d("AuthRepo", "Tables cleared.");
                            } catch (Exception e) {
                                android.util.Log.e("AuthRepo", "Failed to clear tables", e);
                            }
                        }).start();

                        callback.onSuccess();
                    } else {
                        callback.onError("Login failed: missing required auth fields");
                    }
                } else {
                    String errorMsg = "Login failed (" + response.code() + ")";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            android.util.Log.e("AuthRepo", "Login error body: " + errorBody.string());
                        }
                    } catch (IOException ignored) {}
                    callback.onError(errorMsg);
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
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                boolean is2xx = response.code() >= 200 && response.code() <= 299;
                if (is2xx) {
                    AuthResponse auth = response.body();
                    String contentType = response.headers().get("Content-Type");
                    boolean bodyNonNull = auth != null;
                    boolean fieldsPresent = auth != null && auth.getToken() != null && auth.getRefreshToken() != null && auth.getUser() != null;
                    android.util.Log.d("AuthDiagnostic", "HTTP_201: status=" + response.code() + ", contentType=" + contentType + ", bodyNonNull=" + bodyNonNull + ", fieldsPresent=" + fieldsPresent);

                    if (auth == null || auth.getToken() == null || auth.getRefreshToken() == null || auth.getUser() == null || auth.getUser().getId() == null) {
                        android.util.Log.e("AuthDiagnostic", "VALIDATION_FAILED: missing required auth fields");
                        callback.onError("Account created, but the app could not start your session. Please sign in.");
                        return;
                    }

                    try {
                        sessionManager.saveTokens(auth.getToken(), auth.getRefreshToken());
                        sessionManager.saveUser(
                                auth.getUser().getId(),
                                auth.getUser().getFullName(),
                                auth.getUser().getEmail(),
                                auth.getUser().getPhone(),
                                auth.getUser().getRole()
                        );
                        String testToken = sessionManager.getAccessToken();
                        android.util.Log.d("AuthDiagnostic", "Assertion - Read back token present: " + (testToken != null));
                    } catch (Exception e) {
                        android.util.Log.e("AuthDiagnostic", "SESSION_SAVE_FAILED", e);
                        callback.onError("Account created, but your session could not be saved. Please sign in.");
                        return;
                    }

                    new Thread(() -> {
                        try {
                            android.util.Log.d("AuthRepo", "Clearing all tables (register)...");
                            database.clearAllTables();
                        } catch (Exception e) {
                            android.util.Log.e("AuthRepo", "Failed to clear tables (register)", e);
                        }
                    }).start();

                    callback.onSuccess();
                } else {
                    if (response.code() == 409) {
                        callback.onError("Account already exists with this email or phone.");
                    } else {
                        String errorMsg = com.voiceyanga.citizen.core.network.ErrorParser.parseError(response);
                        callback.onError(errorMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (t instanceof com.google.gson.JsonSyntaxException || (t.getLocalizedMessage() != null && t.getLocalizedMessage().toLowerCase().contains("deserialize"))) {
                    android.util.Log.e("AuthDiagnostic", "DESERIALIZATION_FAILED", t);
                    callback.onError("Account created, but the app could not start your session. Please sign in.");
                } else {
                    android.util.Log.e("AuthDiagnostic", "HTTP_REQUEST_FAILED", t);
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    @Override
    public void logout(LogoutCallback callback) {
        // Remove FCM token from backend and local Firebase
        userRepository.removeFcmToken();
        FirebaseMessaging.getInstance().deleteToken();

        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken != null) {
            apiService.logout(new RefreshRequest(refreshToken)).enqueue(new Callback<ApiEnvelope<Object>>() {
                @Override
                public void onResponse(Call<ApiEnvelope<Object>> call, Response<ApiEnvelope<Object>> response) {
                    performLocalLogout(callback);
                }

                @Override
                public void onFailure(Call<ApiEnvelope<Object>> call, Throwable t) {
                    performLocalLogout(callback);
                }
            });
        } else {
            performLocalLogout(callback);
        }
    }

    private void performLocalLogout(LogoutCallback callback) {
        new Thread(() -> {
            database.clearAllTables();
            sessionManager.clearSession();
            callback.onResult(true);
        }).start();
    }

    @Override
    public void resetPassword(String email, LogoutCallback callback) {
        callback.onResult(false);
    }
}
