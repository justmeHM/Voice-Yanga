package com.voiceyanga.citizen.feature.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.voiceyanga.citizen.core.network.ErrorParser;
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
                        .filter(n -> isTypeMatch(type, n.getType()))
                        .collect(java.util.stream.Collectors.toList());
            });
        });
    }

    private static boolean isTypeMatch(String filter, String actualType) {
        if (actualType == null) return false;
        if ("ALL".equals(filter)) return true;
        
        String upperActual = actualType.toUpperCase(java.util.Locale.US);
        
        if ("STATUS_UPDATES".equals(filter) || "COMPLAINT_STATUS_CHANGED".equals(filter)) {
            return upperActual.contains("STATUS") || upperActual.contains("OFFICIAL") || "STATUS_CHANGE".equals(upperActual);
        }
        
        if ("COMMENTS_AND_SUPPORT".equals(filter) || "COMPLAINT_COMMENT_ADDED".equals(filter)) {
            return upperActual.contains("COMMENT") || upperActual.contains("SUPPORT") || upperActual.contains("MILESTONE");
        }
        
        return filter.equalsIgnoreCase(actualType);
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public void setFilterType(String type) {
        filterType.setValue(type);
    }

    public void refresh() {
        _loading.setValue(true);
        new Thread(() -> {
            try {
                repository.refreshNotifications();
                _error.postValue(null);
            } catch (Exception e) {
                _error.postValue(ErrorParser.parseThrowable(e));
            } finally {
                _loading.postValue(false);
            }
        }).start();
    }

    public void markAsRead(String id) {
        repository.markAsRead(id);
    }

    public void markAllAsRead() {
        repository.markAllAsRead();
    }
}