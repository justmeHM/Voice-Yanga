package com.voiceyanga.citizen.core.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.graphics.BitmapFactory;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
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

/**
 * Helper to manage system notifications and persistent notification storage.
 * [Rule 57] Uses string resources for channel metadata.
 */
@Singleton
public class NotificationHelper {

    public static final String CHANNEL_ID = "voice_yanga_alerts";

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
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(String title, String message, String complaintUuid, String type) {
        String keySource = (complaintUuid != null ? complaintUuid : "") + "_" + (type != null ? type : "") + "_" + (title != null ? title : "");
        int rawId = Math.abs(keySource.hashCode());
        final int sysNotifId = rawId == 0 ? 1 : rawId;
        String notificationId = "notif_" + sysNotifId;
        
        // Save to local database with deduplication check
        executorService.execute(() -> {
            Notification existing = notificationDao.getNotificationByIdSync(notificationId);
            if (existing != null) {
                // Already notified recently, skip duplicate banner & DB insert
                return;
            }

            Notification notification = new Notification(
                    notificationId,
                    complaintUuid,
                    title,
                    message,
                    type,
                    System.currentTimeMillis()
            );
            notificationDao.insert(notification);

            // Show system notification
            Intent intent = new Intent(context, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            if (complaintUuid != null) {
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaintUuid);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    sysNotifId, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_voice_yanga))
                    .setColor(ContextCompat.getColor(context, R.color.primary_green))
                    .setContentTitle(title != null ? title : "Voice Yanga")
                    .setContentText(message != null ? message : "")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(sysNotifId, builder.build());
            }
        });
    }

    public void showMilestoneNotification(String title, String complaintTitle, int milestone, String complaintUuid) {
        String message = context.getString(R.string.notification_milestone_message, complaintTitle, milestone);
        showNotification(title, message, complaintUuid, "MILESTONE");
    }
}
