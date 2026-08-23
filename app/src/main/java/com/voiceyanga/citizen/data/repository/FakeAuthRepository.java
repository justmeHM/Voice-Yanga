package com.voiceyanga.citizen.data.repository;

import android.content.Context;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Fake implementation of AuthRepository for development and pilot demonstration.
 * [Rule 55] Mock implementation clearly separated from production potential.
 */
@Singleton
public class FakeAuthRepository implements AuthRepository {

    private final SessionManager sessionManager;
    private final Context context;

    @Inject
    public FakeAuthRepository(SessionManager sessionManager, @ApplicationContext Context context) {
        this.sessionManager = sessionManager;
        this.context = context;
    }

    @Override
    public boolean isUserLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    @Override
    public void login(String email, String password, LoginCallback callback) {
        // Simulate network delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if ("test@citizen.com".equals(email) && "password123".equals(password)) {
                sessionManager.saveToken("mock_token_12345");
                sessionManager.saveUser("Harrison Mwewa", email, "+260 971 123456");
                callback.onSuccess();
            } else {
                callback.onError(context.getString(R.string.error_invalid_credentials));
            }
        }, 1500);
    }

    @Override
    public void register(String firstName, String lastName, String phone, String email, String password, LoginCallback callback) {
        // Simulate network delay for registration
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            sessionManager.saveToken("mock_registered_token_67890");
            sessionManager.saveUser(firstName + " " + lastName, email, phone);
            callback.onSuccess();
        }, 1500);
    }
}
