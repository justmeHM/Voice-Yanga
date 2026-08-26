package com.voiceyanga.citizen.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages user session and authentication tokens securely.
 * [Rule 28] Uses EncryptedSharedPreferences for token security.
 */
@Singleton
public class SessionManager {

    private static final String PREF_NAME = "voice_yanga_secure_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private SharedPreferences prefs;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveTokens(String access, String refresh) {
        android.util.Log.d("SessionManager", "Saving tokens. Access token length: " + (access != null ? access.length() : "null"));
        prefs.edit()
                .putString(KEY_TOKEN, access)
                .putString(KEY_REFRESH_TOKEN, refresh)
                .apply();
    }

    public void saveUser(String name, String email, String phone, String role) {
        String safeName = (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("null null") || name.equalsIgnoreCase("null")) ? "Citizen" : name;
        prefs.edit()
                .putString(KEY_USER_NAME, safeName)
                .putString(KEY_USER_EMAIL, email != null ? email : "")
                .putString(KEY_USER_PHONE, phone != null ? phone : "")
                .putString(KEY_USER_ROLE, role != null ? role : "CITIZEN")
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Citizen");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "CITIZEN");
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearSession() {
        boolean biometricWasEnabled = isBiometricEnabled();
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_PHONE)
                .remove(KEY_USER_ROLE)
                .apply();
        
        // Ensure biometric preference is RESTORED if it was accidentally cleared
        // (Though .remove shouldn't touch it, explicit set is safer for this bug)
        setBiometricEnabled(biometricWasEnabled);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean isFirst) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply();
    }
}
