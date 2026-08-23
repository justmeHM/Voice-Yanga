package com.voiceyanga.citizen.feature.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.SessionManager;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final SessionManager sessionManager;
    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getUpdateSuccess() { return _updateSuccess; }

    @Inject
    public ProfileViewModel(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public String getUserName() { return sessionManager.getUserName(); }
    public String getUserEmail() { return sessionManager.getUserEmail(); }
    public String getUserPhone() { return sessionManager.getUserPhone(); }

    public void updateProfile(String name, String email, String phone) {
        // In a real app, this would call a repository -> API
        sessionManager.saveUser(name, email, phone, sessionManager.getUserRole());
        _updateSuccess.setValue(true);
    }
}