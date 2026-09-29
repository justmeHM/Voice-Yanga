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
            String messageBody = remoteMessage.getNotification().getBody();
            if (messageBody != null) body = messageBody;
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            String dataTitle = remoteMessage.getData().get("title");
            if (dataTitle != null) title = dataTitle;

            String dataBody = remoteMessage.getData().get("body");
            if (dataBody == null) dataBody = remoteMessage.getData().get("message");
            if (dataBody != null) body = dataBody;
            
            complaintUuid = remoteMessage.getData().get("complaintId");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("complaintUuid");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("id");
            if (complaintUuid == null) complaintUuid = remoteMessage.getData().get("complaint_id");
            
            String dataType = remoteMessage.getData().get("type");
            if (dataType != null) type = dataType;
        }
        
        notificationHelper.showNotification(title, body, complaintUuid, type);
        
        // Refresh in-app notifications
        try {
            notificationRepository.refreshNotifications();
        } catch (Exception ignored) {}

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