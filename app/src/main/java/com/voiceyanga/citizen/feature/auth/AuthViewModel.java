package com.voiceyanga.citizen.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    @Inject
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        authRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                loginSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void register(String firstName, String lastName, String phone, String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        authRepository.register(firstName, lastName, phone, email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                loginSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
}