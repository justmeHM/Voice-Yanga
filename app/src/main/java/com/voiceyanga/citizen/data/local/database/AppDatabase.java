package com.voiceyanga.citizen.data.local.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;

@Database(entities = {Complaint.class, Comment.class, ComplaintPhoto.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ComplaintDao complaintDao();
}