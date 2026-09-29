package com.voiceyanga.citizen.data.repository;

import androidx.lifecycle.LiveData;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.NotificationDto;
import com.voiceyanga.citizen.data.remote.dto.PaginatedNotifications;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

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
        executorService.execute(() -> {
            try {
                refreshNotifications();
            } catch (Exception ignored) {}
        });
        return notificationDao.getAllNotifications();
    }

    public void refreshNotifications() throws Exception {
        try {
            int currentPage = 0;
            int totalPages = 1;
            int totalItems = 0;
            int accumulatedParsed = 0;

            while (currentPage < totalPages) {
                // Guide uses getNotifications(page, limit, unreadOnly)
                retrofit2.Response<ApiEnvelope<PaginatedNotifications>> response = apiService.getNotifications(currentPage, 100, false).execute();
                
                int statusCode = response.code();
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    PaginatedNotifications paginatedData = response.body().data;
                    List<NotificationDto> serverNotifications = paginatedData.data;
                    
                    if (paginatedData.meta != null) {
                        totalPages = paginatedData.meta.totalPages;
                        totalItems = paginatedData.meta.totalItems;
                    }

                    android.util.Log.d("NotificationRepo", String.format("NOTIFICATIONS_HTTP page=%d parsedCount=%d totalItems=%d result=%d",
                            currentPage, serverNotifications != null ? serverNotifications.size() : 0, totalItems, statusCode));

                    if (serverNotifications != null) {
                        accumulatedParsed += serverNotifications.size();
                        for (NotificationDto dto : serverNotifications) {
                            Notification notification = new Notification(
                                    dto.getId(),
                                    dto.getComplaintId(), // Store server complaint ID as requested
                                    dto.getTitle(),
                                    dto.getMessage(),
                                    dto.getType(),
                                    com.voiceyanga.citizen.data.repository.ComplaintRepository.parseServerDate(dto.getCreatedAt())
                            );
                            notification.setRead(dto.isRead());
                            notificationDao.insert(notification);
                        }
                    }
                    currentPage++;
                } else {
                    android.util.Log.e("NotificationRepo", "NOTIFICATIONS_HTTP result=" + statusCode);
                    throw new Exception("Server returned " + statusCode);
                }
            }
            
            // DB status logging
            int observedCount = notificationDao.getCountSync();
            int unreadCount = notificationDao.getUnreadCountSync();
            android.util.Log.d("NotificationRepo", String.format("NOTIFICATIONS_DB upserted=%d observedCount=%d unreadCount=%d",
                    accumulatedParsed, observedCount, unreadCount));
            
        } catch (Exception e) {
            android.util.Log.e("NotificationRepo", "Refresh failed", e);
            throw e;
        }
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
