package com.voiceyanga.citizen.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.repository.AuthRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SplashViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>();

    @Inject
    public SplashViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void checkSession() {
        isLoggedIn.setValue(authRepository.isUserLoggedIn());
    }

    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedIn;
    }
}