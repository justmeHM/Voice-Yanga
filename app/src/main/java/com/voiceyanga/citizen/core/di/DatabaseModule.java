package com.voiceyanga.citizen.core.di;

import android.content.Context;
import androidx.room.Room;
import com.voiceyanga.citizen.data.local.dao.ComplaintDao;
import com.voiceyanga.citizen.data.local.dao.NotificationDao;
import com.voiceyanga.citizen.data.local.database.AppDatabase;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "voice_yanga_db")
                .addMigrations(
                        AppDatabase.MIGRATION_6_7, 
                        AppDatabase.MIGRATION_7_8, 
                        AppDatabase.MIGRATION_8_9,
                        AppDatabase.MIGRATION_9_10,
                        AppDatabase.MIGRATION_10_11,
                        AppDatabase.MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public ComplaintDao provideComplaintDao(AppDatabase database) {
        return database.complaintDao();
    }

    @Provides
    public NotificationDao provideNotificationDao(AppDatabase database) {
        return database.notificationDao();
    }
}