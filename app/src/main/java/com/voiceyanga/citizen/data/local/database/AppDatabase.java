package com.voiceyanga.citizen.data.local.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.local.entity.Notification;

@Database(entities = {Complaint.class, Comment.class, ComplaintPhoto.class, Notification.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ComplaintDao complaintDao();
    public abstract NotificationDao notificationDao();
}