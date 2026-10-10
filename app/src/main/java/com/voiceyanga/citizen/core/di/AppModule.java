package com.voiceyanga.citizen.core.di;

import android.content.Context;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public WorkManager provideWorkManager(@ApplicationContext Context context) {
        try {
            return WorkManager.getInstance(context);
        } catch (IllegalStateException e) {
            try {
                Context appContext = context.getApplicationContext();
                if (appContext instanceof Configuration.Provider) {
                    Configuration config = ((Configuration.Provider) appContext).getWorkManagerConfiguration();
                    WorkManager.initialize(appContext, config);
                } else {
                    WorkManager.initialize(appContext, new Configuration.Builder().build());
                }
                return WorkManager.getInstance(appContext);
            } catch (Throwable t) {
                android.util.Log.e("AppModule", "Failed to initialize WorkManager", t);
                return null;
            }
        }
    }
}
