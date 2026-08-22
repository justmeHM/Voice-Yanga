package com.voiceyanga.citizen;

import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import com.voiceyanga.citizen.feature.notifications.NotificationViewModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NotificationViewModelTest {

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private NotificationRepository repository;

    private NotificationViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        viewModel = new NotificationViewModel(repository);
    }

    @Test
    public void testMarkAsRead() {
        String id = "notif_1";
        viewModel.markAsRead(id);
        verify(repository).markAsRead(id);
    }

    @Test
    public void testMarkAllAsRead() {
        viewModel.markAllAsRead();
        verify(repository).markAllAsRead();
    }
}
