package com.voiceyanga.citizen.core.notifications;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import com.voiceyanga.citizen.data.repository.NotificationRepository;
import com.voiceyanga.citizen.data.repository.UserRepository;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoiceYangaMessagingService extends FirebaseMessagingService {

    @Inject
    NotificationHelper notificationHelper;

    @Inject
    UserRepository userRepository;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    ComplaintRepository complaintRepository;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Voice Yanga";
        String body = "You have a new update.";
        String complaintUuid = null;
        String type = "SYSTEM";

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body")) body = remoteMessage.getData().get("body");
            if (remoteMessage.getData().containsKey("message")) body = remoteMessage.getData().get("message");
            
            complaintUuid = remoteMessage.getData().get("complaintId");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("complaintUuid");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("id");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("complaint_id");
            
            type = remoteMessage.getData().get("type");
            if (type == null) type = "SYSTEM";
        }
        
        notificationHelper.showNotification(title, body, complaintUuid, type);
        
        // Refresh in-app notifications
        notificationRepository.refreshNotifications();

        // Proactive refresh for real-time UI updates
        if (complaintUuid != null) {
            complaintRepository.refreshComplaintDetail(complaintUuid);
        } else {
            complaintRepository.refreshComplaints();
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        userRepository.registerFcmToken(token);
    }
}