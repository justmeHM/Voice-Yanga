package com.voiceyanga.citizen.core.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.feature.auth.SplashActivity;
import com.voiceyanga.citizen.feature.complaints.ComplaintDetailActivity;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NotificationHelper {

    public static final String CHANNEL_ID = "voice_yanga_alerts";
    private static final String CHANNEL_NAME = "Voice Yanga Alerts";
    private static final String CHANNEL_DESC = "Notifications for complaint status changes and updates.";

    private final Context context;
    private final NotificationDao notificationDao;
    private final ExecutorService executorService;

    @Inject
    public NotificationHelper(@ApplicationContext Context context, NotificationDao notificationDao) {
        this.context = context;
        this.notificationDao = notificationDao;
        this.executorService = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(String title, String message, String complaintUuid, String type) {
        String notificationId = UUID.randomUUID().toString();
        
        // Save to local database
        executorService.execute(() -> {
            Notification notification = new Notification(
                    notificationId,
                    complaintUuid,
                    title,
                    message,
                    type,
                    System.currentTimeMillis()
            );
            notificationDao.insert(notification);
        });

        // Show system notification
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (complaintUuid != null) {
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaintUuid);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell_vector)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}