package com.voiceyanga.citizen.domain.repository;

/**
 * Interface for handling authentication logic.
 * [Rule 55] Separates production contract from implementation.
 */
public interface AuthRepository {
    boolean isUserLoggedIn();
    void login(String email, String password, LoginCallback callback);
    void register(String firstName, String lastName, String phone, String email, String password, LoginCallback callback);
    void logout(LogoutCallback callback);
    void resetPassword(String email, LogoutCallback callback);

    interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }

    interface LogoutCallback {
        void onResult(boolean success);
    }
}
