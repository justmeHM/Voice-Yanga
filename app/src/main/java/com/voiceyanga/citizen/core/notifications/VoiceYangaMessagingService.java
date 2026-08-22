package com.voiceyanga.citizen.core.notifications;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoiceYangaMessagingService extends FirebaseMessagingService {

    @Inject
    NotificationHelper notificationHelper;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Extract data if present
            String complaintUuid = remoteMessage.getData().get("complaintUuid");
            String type = remoteMessage.getData().get("type");
            
            notificationHelper.showNotification(title, body, complaintUuid, type != null ? type : "SYSTEM");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // In a real app, send this token to the server
    }
}