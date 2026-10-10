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
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ANONYMOUS_USER_ID = "anonymous_user_id";
    private static final String KEY_SESSION_ID = "session_id";

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
        } catch (Throwable e) {
            android.util.Log.e("SessionManager", "EncryptedSharedPreferences creation failed, falling back to standard prefs", e);
            try {
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                keyStore.deleteEntry("_androidx_security_master_key_");
            } catch (Throwable ignored) {}

            try {
                context.deleteSharedPreferences(PREF_NAME);
            } catch (Throwable ignored) {}

            try {
                prefs = context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
            } catch (Throwable t) {
                android.util.Log.e("SessionManager", "Fallback SharedPreferences creation failed", t);
            }
        }

        if (prefs == null) {
            try {
                prefs = context.getSharedPreferences("voice_yanga_safe_session", Context.MODE_PRIVATE);
            } catch (Throwable ignored) {}
        }
    }

    public void saveTokens(String access, String refresh) {
        if (prefs == null) return;
        try {
            android.util.Log.d("SessionManager", "Saving tokens. Access token length: " + (access != null ? access.length() : "null"));
            SharedPreferences.Editor editor = prefs.edit();
            if (access != null) {
                editor.putString(KEY_TOKEN, access);
            } else {
                editor.remove(KEY_TOKEN);
            }
            
            if (refresh != null) {
                editor.putString(KEY_REFRESH_TOKEN, refresh);
            } else {
                editor.remove(KEY_REFRESH_TOKEN);
            }
            editor.commit();
        } catch (Throwable t) {
            android.util.Log.e("SessionManager", "Error saving tokens", t);
        }
    }

    public void saveUser(String id, String name, String email, String phone, String role) {
        if (prefs == null) return;
        try {
            String safeName = (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("null null") || name.equalsIgnoreCase("null")) ? "Citizen" : name;
            prefs.edit()
                    .putString(KEY_USER_ID, id != null ? id : "")
                    .putString(KEY_USER_NAME, safeName)
                    .putString(KEY_USER_EMAIL, email != null ? email : "")
                    .putString(KEY_USER_PHONE, phone != null ? phone : "")
                    .putString(KEY_USER_ROLE, role != null ? role : "CITIZEN")
                    .apply();
        } catch (Throwable t) {
            android.util.Log.e("SessionManager", "Error saving user info", t);
        }
    }

    public String getUserId() {
        if (prefs == null) return "";
        try {
            return prefs.getString(KEY_USER_ID, "");
        } catch (Throwable t) {
            return "";
        }
    }

    public String getAccessToken() {
        if (prefs == null) return null;
        try {
            return prefs.getString(KEY_TOKEN, null);
        } catch (Throwable t) {
            android.util.Log.e("SessionManager", "Error reading access token", t);
            return null;
        }
    }

    public String getRefreshToken() {
        if (prefs == null) return null;
        try {
            return prefs.getString(KEY_REFRESH_TOKEN, null);
        } catch (Throwable t) {
            android.util.Log.e("SessionManager", "Error reading refresh token", t);
            return null;
        }
    }

    public String getUserName() {
        if (prefs == null) return "Citizen";
        try {
            return prefs.getString(KEY_USER_NAME, "Citizen");
        } catch (Throwable t) {
            return "Citizen";
        }
    }

    public String getUserEmail() {
        if (prefs == null) return "";
        try {
            return prefs.getString(KEY_USER_EMAIL, "");
        } catch (Throwable t) {
            return "";
        }
    }

    public String getUserPhone() {
        if (prefs == null) return "";
        try {
            return prefs.getString(KEY_USER_PHONE, "");
        } catch (Throwable t) {
            return "";
        }
    }

    public String getUserRole() {
        if (prefs == null) return "CITIZEN";
        try {
            return prefs.getString(KEY_USER_ROLE, "CITIZEN");
        } catch (Throwable t) {
            return "CITIZEN";
        }
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clearSession() {
        boolean biometricWasEnabled = isBiometricEnabled();
        if (prefs != null) {
            try {
                prefs.edit()
                        .remove(KEY_TOKEN)
                        .remove(KEY_REFRESH_TOKEN)
                        .remove(KEY_USER_ID)
                        .remove(KEY_USER_NAME)
                        .remove(KEY_USER_EMAIL)
                        .remove(KEY_USER_PHONE)
                        .remove(KEY_USER_ROLE)
                        .apply();
            } catch (Throwable ignored) {}
        }
        setBiometricEnabled(biometricWasEnabled);
    }

    public void setBiometricEnabled(boolean enabled) {
        if (prefs == null) return;
        try {
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
        } catch (Throwable ignored) {}
    }

    public boolean isBiometricEnabled() {
        if (prefs == null) return false;
        try {
            return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isFirstLaunch() {
        if (prefs == null) return true;
        try {
            return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        } catch (Throwable t) {
            return true;
        }
    }

    public void setFirstLaunch(boolean isFirst) {
        if (prefs == null) return;
        try {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply();
        } catch (Throwable ignored) {}
    }

    public synchronized String getAnonymousUserId() {
        if (prefs == null) return java.util.UUID.randomUUID().toString();
        try {
            String id = prefs.getString(KEY_ANONYMOUS_USER_ID, null);
            if (id == null || id.isEmpty()) {
                id = java.util.UUID.randomUUID().toString();
                prefs.edit().putString(KEY_ANONYMOUS_USER_ID, id).commit();
            }
            return id;
        } catch (Throwable t) {
            return java.util.UUID.randomUUID().toString();
        }
    }

    public synchronized String getSessionId() {
        if (prefs == null) return java.util.UUID.randomUUID().toString();
        try {
            String id = prefs.getString(KEY_SESSION_ID, null);
            if (id == null || id.isEmpty()) {
                id = java.util.UUID.randomUUID().toString();
                prefs.edit().putString(KEY_SESSION_ID, id).commit();
            }
            return id;
        } catch (Throwable t) {
            return java.util.UUID.randomUUID().toString();
        }
    }

    public synchronized String rotateSessionId() {
        String newSessionId = java.util.UUID.randomUUID().toString();
        if (prefs != null) {
            try {
                prefs.edit().putString(KEY_SESSION_ID, newSessionId).commit();
            } catch (Throwable ignored) {}
        }
        return newSessionId;
    }

    public synchronized void clearAnonymousUserId() {
        if (prefs == null) return;
        try {
            prefs.edit().remove(KEY_ANONYMOUS_USER_ID).remove(KEY_SESSION_ID).apply();
        } catch (Throwable ignored) {}
    }
}
