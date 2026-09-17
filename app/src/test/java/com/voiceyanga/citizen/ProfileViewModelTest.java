package com.voiceyanga.citizen;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
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
    private Call<Void> voidCall;

    @Mock
    private Call<com.voiceyanga.citizen.data.remote.dto.UserDto> userCall;

    private ProfileViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(apiService.getProfile()).thenReturn(userCall);
        viewModel = new ProfileViewModel(sessionManager, apiService);
    }

    @Test
    @org.junit.Ignore
    public void testDeleteAccountSuccess() {
        // when(apiService.deleteAccount()).thenReturn(voidCall);

        // Mock the enqueue to trigger onResponse immediately
        Answer<Void> answer = invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(voidCall, Response.success(null));
            return null;
        };
        org.mockito.Mockito.doAnswer(answer).when(voidCall).enqueue(any());

        viewModel.deleteAccount();

        verify(sessionManager).clearSession();
        assertTrue(viewModel.getDeleteSuccess().getValue());
    }
}