package com.voiceyanga.citizen.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.entity.Comment;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.data.local.entity.Notification;
import com.voiceyanga.citizen.data.local.entity.PendingAction;

@Database(entities = {Complaint.class, Comment.class, ComplaintPhoto.class, Notification.class, PendingAction.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ComplaintDao complaintDao();
    public abstract NotificationDao notificationDao();

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE complaints ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE complaints ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE complaints ADD COLUMN supportedByMe INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Re-create the index with unique constraint
            database.execSQL("DROP INDEX IF EXISTS index_comments_complaintUuid");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_comments_complaintUuid_authorName_content ON comments(complaintUuid, authorName, content)");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE complaints ADD COLUMN commentCount INTEGER NOT NULL DEFAULT 0");
        }
    };
}
