package com.voiceyanga.citizen.data.local.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.entity.Complaint;

@Database(entities = {Complaint.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ComplaintDao complaintDao();
}