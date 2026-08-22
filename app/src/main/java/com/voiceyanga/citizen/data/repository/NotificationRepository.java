package com.voiceyanga.citizen.data.repository;

import androidx.lifecycle.LiveData;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Notification;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationRepository {

    private final NotificationDao notificationDao;
    private final ExecutorService executorService;

    @Inject
    public NotificationRepository(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Notification>> getAllNotifications() {
        return notificationDao.getAllNotifications();
    }

    public LiveData<Integer> getUnreadCount() {
        return notificationDao.getUnreadCount();
    }

    public void markAsRead(String id) {
        executorService.execute(() -> notificationDao.markAsRead(id));
    }

    public void markAllAsRead() {
        executorService.execute(notificationDao::markAllAsRead);
    }
}