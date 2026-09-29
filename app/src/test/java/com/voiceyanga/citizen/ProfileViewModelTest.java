package com.voiceyanga.citizen;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.feature.profile.ProfileViewModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ProfileViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private SessionManager sessionManager;

    @Mock
    private ApiService apiService;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private Call<Void> voidCall;

    @Mock
    private Call<com.voiceyanga.citizen.data.remote.dto.UserDto> userCall;

    private ProfileViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(apiService.getProfile()).thenReturn(userCall);
        viewModel = new ProfileViewModel(sessionManager, apiService, authRepository);
    }

    @Test
    public void testDeleteAccountSuccess() {
        Answer<Void> answer = invocation -> {
            AuthRepository.LogoutCallback callback = invocation.getArgument(0);
            callback.onResult(true);
            return null;
        };
        org.mockito.Mockito.doAnswer(answer).when(authRepository).deleteAccount(any());

        viewModel.deleteAccount();

        assertTrue(viewModel.getDeleteSuccess().getValue());
    }
}
