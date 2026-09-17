package com.voiceyanga.citizen.feature.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.UserDto;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final SessionManager sessionManager;
    private final ApiService apiService;
    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getUpdateSuccess() { return _updateSuccess; }

    private final MutableLiveData<Boolean> _deleteSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getDeleteSuccess() { return _deleteSuccess; }

    private final MutableLiveData<UserDto> _userProfile = new MutableLiveData<>();
    public LiveData<UserDto> getUserProfile() { return _userProfile; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    @Inject
    public ProfileViewModel(SessionManager sessionManager, ApiService apiService) {
        this.sessionManager = sessionManager;
        this.apiService = apiService;
        
        UserDto cached = new UserDto();
        String fullName = sessionManager.getUserName();
        if (fullName != null && fullName.contains(" ")) {
            int firstSpace = fullName.indexOf(" ");
            cached.setFirstName(fullName.substring(0, firstSpace));
            cached.setLastName(fullName.substring(firstSpace + 1).trim());
        } else {
            cached.setFirstName(fullName != null ? fullName : "Citizen");
            cached.setLastName("");
        }
        cached.setEmail(sessionManager.getUserEmail());
        cached.setPhone(sessionManager.getUserPhone());
        _userProfile.setValue(cached);

        fetchProfile();
    }

    public void fetchProfile() {
        apiService.getProfile().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto user = response.body();
                    _userProfile.setValue(user);
                    if (user.getEmail() != null) {
                        sessionManager.saveUser(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            user.getRole()
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                _error.setValue(t.getMessage());
            }
        });
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("phone", phone);

        apiService.updateProfile(updates).enqueue(new Callback<ApiEnvelope<UserDto>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<UserDto>> call, Response<ApiEnvelope<UserDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    _updateSuccess.setValue(true);
                    fetchProfile();
                } else {
                    _error.setValue("Update failed");
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<UserDto>> call, Throwable t) {
                _error.setValue(t.getMessage());
            }
        });
    }

    public void deleteAccount() {
        // Not supported by current API guide
        _error.setValue("Delete account not supported");
    }

    public void registerFcmToken(String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);

        apiService.updateProfile(updates).enqueue(new Callback<ApiEnvelope<UserDto>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<UserDto>> call, Response<ApiEnvelope<UserDto>> response) {
                android.util.Log.d("ProfileVM", "FCM Token registered: " + response.isSuccessful());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<UserDto>> call, Throwable t) {
                android.util.Log.e("ProfileVM", "FCM Token registration failed", t);
            }
        });
    }
}
