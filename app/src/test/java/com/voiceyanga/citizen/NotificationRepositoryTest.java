package com.voiceyanga.citizen;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NotificationRepositoryTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private ApiService apiService;

    @Mock
    private retrofit2.Call<Void> voidCall;

    private NotificationRepository repository;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new NotificationRepository(notificationDao, apiService);
        
        // Default mocks for API calls
        when(apiService.markNotificationRead(anyString())).thenReturn(voidCall);
        when(apiService.markAllNotificationsRead()).thenReturn(voidCall);
    }

    @Test
    public void testMarkAsRead() throws InterruptedException {
        String id = "notif_1";
        repository.markAsRead(id);
        
        // Wait for executor - using a longer sleep for reliability in CI
        Thread.sleep(500);
        
        verify(notificationDao).markAsRead(id);
    }

    @Test
    public void testMarkAllAsRead() throws InterruptedException {
        repository.markAllAsRead();
        
        // Wait for executor
        Thread.sleep(500);
        
        verify(notificationDao).markAllAsRead();
    }
}
