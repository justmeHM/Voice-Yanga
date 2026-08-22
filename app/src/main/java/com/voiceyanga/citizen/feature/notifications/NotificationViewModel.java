package com.voiceyanga.citizen.feature.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;

    @Inject
    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Notification>> getNotifications() {
        return repository.getAllNotifications();
    }

    public void markAsRead(String id) {
        repository.markAsRead(id);
    }

    public void markAllAsRead() {
        repository.markAllAsRead();
    }
}