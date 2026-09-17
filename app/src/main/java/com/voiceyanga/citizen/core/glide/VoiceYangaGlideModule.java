package com.voiceyanga.citizen.core.glide;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import java.io.InputStream;
import okhttp3.OkHttpClient;

@GlideModule
public final class VoiceYangaGlideModule extends AppGlideModule {

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    public interface GlideModuleEntryPoint {
        OkHttpClient getOkHttpClient();
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // Use Hilt's EntryPoint to get the authenticated OkHttpClient
        GlideModuleEntryPoint entryPoint = EntryPointAccessors.fromApplication(
                context.getApplicationContext(), 
                GlideModuleEntryPoint.class
        );
        
        OkHttpClient client = entryPoint.getOkHttpClient();
        
        registry.replace(
                GlideUrl.class, 
                InputStream.class, 
                new OkHttpUrlLoader.Factory(client)
        );
    }
}
