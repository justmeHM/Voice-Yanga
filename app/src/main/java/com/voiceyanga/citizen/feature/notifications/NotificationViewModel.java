package com.voiceyanga.citizen.feature.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<String> filterType = new MutableLiveData<>("ALL");
    private final LiveData<List<Notification>> notifications;

    @Inject
    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;
        this.notifications = Transformations.switchMap(filterType, type -> {
            LiveData<List<Notification>> all = repository.getAllNotifications();
            if ("ALL".equals(type)) return all;
            return Transformations.map(all, list -> {
                if (list == null) return null;
                return list.stream()
                        .filter(n -> type.equals(n.getType()))
                        .collect(java.util.stream.Collectors.toList());
            });
        });
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public void setFilterType(String type) {
        filterType.setValue(type);
    }

    public void markAsRead(String id) {
        repository.markAsRead(id);
    }

    public void markAllAsRead() {
        repository.markAllAsRead();
    }
}