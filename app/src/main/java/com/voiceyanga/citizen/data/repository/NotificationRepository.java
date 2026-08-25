package com.voiceyanga.citizen.data.repository;

import androidx.lifecycle.LiveData;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.BaseResponse;
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
                Response<BaseResponse<PaginatedResponse<Notification>>> response = apiService.getNotifications().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Notification> serverNotifications = response.body().getData().getData();
                    if (serverNotifications != null) {
                        for (Notification notification : serverNotifications) {
                            notificationDao.insert(notification);
                        }
                    }
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
