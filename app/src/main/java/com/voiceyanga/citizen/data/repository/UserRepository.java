package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.NotificationPreferences;
import com.voiceyanga.citizen.data.remote.dto.UserDto;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class UserRepository {
    private final ApiService apiService;
    private final SessionManager sessionManager;

    @Inject
    public UserRepository(ApiService apiService, SessionManager sessionManager) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
    }

    public void registerFcmToken(String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);

        apiService.updateProfile(updates).enqueue(new Callback<ApiEnvelope<UserDto>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<UserDto>> call, Response<ApiEnvelope<UserDto>> response) {
                android.util.Log.d("UserRepo", "FCM Token registered: " + response.isSuccessful());
                if (response.isSuccessful()) {
                    // Automatically enable push notifications when a token is registered
                    updateNotificationPreferences(new NotificationPreferences(true, true, false, true, true));
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<UserDto>> call, Throwable t) {
                android.util.Log.e("UserRepo", "FCM Token registration failed", t);
            }
        });
    }

    public void updateNotificationPreferences(NotificationPreferences preferences) {
        apiService.updateNotificationPreferences(preferences).enqueue(new Callback<ApiEnvelope<Object>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<Object>> call, Response<ApiEnvelope<Object>> response) {
                android.util.Log.d("UserRepo", "Notification preferences updated: " + response.isSuccessful());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<Object>> call, Throwable t) {
                android.util.Log.e("UserRepo", "Notification preferences update failed", t);
            }
        });
    }

    public void getProfile(Callback<UserDto> callback) {
        apiService.getProfile().enqueue(callback);
    }

    public void removeFcmToken() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", null);

        apiService.updateProfile(updates).enqueue(new Callback<ApiEnvelope<UserDto>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<UserDto>> call, Response<ApiEnvelope<UserDto>> response) {
                android.util.Log.d("UserRepo", "FCM Token removed: " + response.isSuccessful());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<UserDto>> call, Throwable t) {
                android.util.Log.e("UserRepo", "FCM Token removal failed", t);
            }
        });
    }
}
