package com.voiceyanga.citizen.feature.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.messaging.FirebaseMessaging;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.data.repository.UserRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);
    private boolean registrationNavigationDone = false;

    public boolean isRegistrationNavigationDone() {
        return registrationNavigationDone;
    }

    public void markRegistrationNavigationDone() {
        this.registrationNavigationDone = true;
    }

    @Inject
    public AuthViewModel(AuthRepository authRepository, UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        authRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                syncFcmToken();
                isLoading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void register(String firstName, String lastName, String phone, String email, String password) {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            android.util.Log.d("AuthViewModel", "Registration request already in-flight. Ignoring duplicate submission.");
            return;
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);
        authRepository.register(firstName, lastName, phone, email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                syncFcmToken();
                isLoading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    private void syncFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                userRepository.registerFcmToken(task.getResult());
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