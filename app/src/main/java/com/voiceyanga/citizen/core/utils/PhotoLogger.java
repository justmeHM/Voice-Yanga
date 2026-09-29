package com.voiceyanga.citizen.core.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.voiceyanga.citizen.BuildConfig;

public class PhotoLogger {

    public static <T> RequestListener<T> listener() {
        return listener("unknown", "unknown");
    }

    public static <T> RequestListener<T> listener(String serverId, String mediaId) {
        return new RequestListener<T>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<T> target, boolean isFirstResource) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("PHOTO_UI", String.format("source=remote result=failure serverId=%s mediaId=%s", serverId, mediaId));
                }
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull T resource, Object model, Target<T> target, DataSource dataSource, boolean isFirstResource) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("PHOTO_UI", String.format("source=remote result=rendered serverId=%s mediaId=%s", serverId, mediaId));
                }
                return false;
            }
        };
    }

    public static void logFailure(String serverId, String mediaId, String reason) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("PHOTO_UI", String.format("source=remote result=failure serverId=%s mediaId=%s reason=%s", serverId, mediaId, reason));
        }
    }

    public static void logPlaceholder() {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("PHOTO_UI", "source=local result=placeholder");
        }
    }

    private static boolean isRemote(Object model) {
        if (model == null) return false;
        String s = model.toString();
        return s.startsWith("http://") || s.startsWith("https://");
    }
}
