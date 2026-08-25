package com.voiceyanga.citizen.data.repository;

import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
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

        apiService.updateProfile(updates).enqueue(new Callback<BaseResponse<UserDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<UserDto>> call, Response<BaseResponse<UserDto>> response) {
                android.util.Log.d("UserRepo", "FCM Token registered: " + response.isSuccessful());
            }

            @Override
            public void onFailure(Call<BaseResponse<UserDto>> call, Throwable t) {
                android.util.Log.e("UserRepo", "FCM Token registration failed", t);
            }
        });
    }
}