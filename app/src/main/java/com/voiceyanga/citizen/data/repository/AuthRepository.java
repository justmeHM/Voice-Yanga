package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.local.SessionManager;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for handling authentication logic.
 * [FR-AUTH-01] Login implementation.
 * [FR-AUTH-02] Registration implementation.
 */
@Singleton
public class AuthRepository {

    private final SessionManager sessionManager;

    @Inject
    public AuthRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void login(String email, String password, LoginCallback callback) {
        // Simulate network delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if ("test@citizen.com".equals(email) && "password123".equals(password)) {
                sessionManager.saveToken("mock_token_12345");
                sessionManager.saveUser("John Doe", email, "+260 971 123456");
                callback.onSuccess();
            } else {
                callback.onError("Invalid credentials");
            }
        }, 2000);
    }

    public void register(String firstName, String lastName, String phone, String email, String password, LoginCallback callback) {
        // Simulate network delay for registration
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Mock registration success
            sessionManager.saveToken("mock_registered_token_67890");
            sessionManager.saveUser(firstName + " " + lastName, email, phone);
            callback.onSuccess();
        }, 2000);
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }
}