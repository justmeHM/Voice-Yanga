package com.voiceyanga.citizen.data.repository;

import androidx.lifecycle.LiveData;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
import com.voiceyanga.citizen.data.remote.dto.NotificationDto;
import com.voiceyanga.citizen.data.remote.dto.PaginatedResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Response;

@Singleton
public class NotificationRepository {

    private final NotificationDao notificationDao;
    private final ApiService apiService;
    private final ExecutorService executorService;

    @Inject
    public NotificationRepository(NotificationDao notificationDao, ApiService apiService) {
        this.notificationDao = notificationDao;
        this.apiService = apiService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Notification>> getAllNotifications() {
        refreshNotifications();
        return notificationDao.getAllNotifications();
    }

    private void refreshNotifications() {
        executorService.execute(() -> {
            try {
                Response<List<NotificationDto>> response = apiService.getNotifications().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationDto> serverNotifications = response.body();
                    if (serverNotifications != null) {
                        for (NotificationDto dto : serverNotifications) {
                            Notification notification = new Notification(
                                    dto.getId(),
                                    dto.getComplaintUuid(),
                                    dto.getTitle(),
                                    dto.getMessage(),
                                    dto.getType(),
                                    dto.getTimestamp()
                            );
                            notification.setRead(dto.isRead());
                            notificationDao.insert(notification);
                        }
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    android.util.Log.e("NotificationRepo", "Refresh failed (" + response.code() + "): " + errorBody);
                }
            } catch (Exception e) {
                android.util.Log.e("NotificationRepo", "Refresh failed", e);
            }
        });
    }

    public LiveData<Integer> getUnreadCount() {
        return notificationDao.getUnreadCount();
    }

    public void markAsRead(String id) {
        executorService.execute(() -> {
            notificationDao.markAsRead(id);
            try {
                apiService.markNotificationRead(id).execute();
            } catch (Exception e) {
                android.util.Log.e("NotificationRepo", "Mark read sync failed", e);
            }
        });
    }

    public void markAllAsRead() {
        executorService.execute(() -> {
            notificationDao.markAllAsRead();
            try {
                apiService.markAllNotificationsRead().execute();
            } catch (Exception e) {
                android.util.Log.e("NotificationRepo", "Mark all read sync failed", e);
            }
        });
    }
}
