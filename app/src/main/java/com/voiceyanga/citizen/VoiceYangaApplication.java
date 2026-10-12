package com.voiceyanga.citizen;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import javax.inject.Inject;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class VoiceYangaApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if (sessionManager != null) {
            sessionManager.applyTheme();
        }
        // Prevent Glide from throwing "You must not call setTag() on a view Glide is targeting"
        com.bumptech.glide.request.target.ViewTarget.setTagId(R.id.glide_internal_request_tag);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channelAlerts = new NotificationChannel(
                        "voice_yanga_alerts",
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channelAlerts.setDescription(getString(R.string.notification_channel_desc));
                manager.createNotificationChannel(channelAlerts);

                NotificationChannel channelUpdates = new NotificationChannel(
                        "complaint_updates",
                        "Complaint updates",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channelUpdates.setDescription("Responses and updates to your complaints");
                manager.createNotificationChannel(channelUpdates);
            }
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        if (workerFactory != null) {
            return new Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build();
        } else {
            return new Configuration.Builder().build();
        }
    }
}