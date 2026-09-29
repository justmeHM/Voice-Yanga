package com.voiceyanga.citizen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.work.WorkManager;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.local.database.AppDatabase;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.DeleteAccountResponse;
import com.voiceyanga.citizen.data.repository.RealAuthRepository;
import com.voiceyanga.citizen.data.repository.UserRepository;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.feature.profile.ProfileViewModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountDeletionTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private ApiService apiService;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private AppDatabase database;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkManager workManager;

    @Mock
    private Call<ApiEnvelope<DeleteAccountResponse>> deleteCall;

    @Mock
    private AuthRepository.LogoutCallback logoutCallback;

    private RealAuthRepository authRepository;
    private ProfileViewModel profileViewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        authRepository = new RealAuthRepository(apiService, sessionManager, database, userRepository, workManager);
        
        when(apiService.getProfile()).thenReturn(mock(Call.class));
        profileViewModel = new ProfileViewModel(sessionManager, apiService, authRepository);
        
        when(apiService.deleteMyAccount()).thenReturn(deleteCall);
    }

    @Test
    public void testDeleteAccountSuccessClearsData() {
        // Mock API success
        ArgumentCaptor<Callback<ApiEnvelope<DeleteAccountResponse>>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        
        authRepository.deleteAccount(logoutCallback);
        
        verify(apiService).deleteMyAccount();
        verify(deleteCall).enqueue(callbackCaptor.capture());
        
        ApiEnvelope<DeleteAccountResponse> envelope = new ApiEnvelope<>();
        envelope.success = true;
        envelope.data = new DeleteAccountResponse();
        envelope.data.setMessage("Account deleted permanently");
        
        callbackCaptor.getValue().onResponse(deleteCall, Response.success(envelope));
        
        // Cleanup happens on a new thread
        verify(workManager, timeout(1000)).cancelAllWorkByTag("complaint_sync");
        verify(database, timeout(1000)).clearAllTables();
        verify(sessionManager, timeout(1000)).clearSession();
        verify(logoutCallback, timeout(1000)).onResult(true);
    }

    @Test
    public void testDeleteAccount401AlsoClearsData() {
        ArgumentCaptor<Callback<ApiEnvelope<DeleteAccountResponse>>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        
        authRepository.deleteAccount(logoutCallback);
        
        verify(deleteCall).enqueue(callbackCaptor.capture());
        
        callbackCaptor.getValue().onResponse(deleteCall, Response.error(401, okhttp3.ResponseBody.create(null, "")));
        
        verify(database, timeout(1000)).clearAllTables();
        verify(sessionManager, timeout(1000)).clearSession();
        verify(logoutCallback, timeout(1000)).onResult(true);
    }

    @Test
    public void testDeleteAccountFailurePreservesData() {
        ArgumentCaptor<Callback<ApiEnvelope<DeleteAccountResponse>>> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        
        authRepository.deleteAccount(logoutCallback);
        
        verify(deleteCall).enqueue(callbackCaptor.capture());
        
        callbackCaptor.getValue().onFailure(deleteCall, new Exception("Network error"));
        
        verify(database, never()).clearAllTables();
        verify(sessionManager, never()).clearSession();
        verify(logoutCallback).onResult(false);
    }

    @Test
    public void testProfileViewModelPreventsDuplicateTaps() {
        profileViewModel.deleteAccount();
        profileViewModel.deleteAccount();
        
        // Should only call repository once because isDeleting becomes true
        verify(deleteCall).enqueue(any());
    }
}
