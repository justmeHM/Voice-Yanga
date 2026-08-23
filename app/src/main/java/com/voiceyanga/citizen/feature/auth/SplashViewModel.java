package com.voiceyanga.citizen.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.HealthResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SplashViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final ApiService apiService;
    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isServerReachable = new MutableLiveData<>();

    @Inject
    public SplashViewModel(AuthRepository authRepository, ApiService apiService) {
        this.authRepository = authRepository;
        this.apiService = apiService;
    }

    public void checkSession() {
        isLoggedIn.setValue(authRepository.isUserLoggedIn());
    }

    public void checkServerHealth() {
        apiService.healthCheck().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                isServerReachable.setValue(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                isServerReachable.setValue(false);
            }
        });
    }

    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedIn;
    }

    public LiveData<Boolean> getIsServerReachable() {
        return isServerReachable;
    }
}