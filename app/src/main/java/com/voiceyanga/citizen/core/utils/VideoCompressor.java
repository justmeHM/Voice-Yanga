package com.voiceyanga.citizen.core.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;
import com.voiceyanga.citizen.core.network.ErrorParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class VideoCompressor {

    public interface CompressCallback {
        void onSuccess(File compressedFile);
        void onError(String errorMessage);
    }

    public static void compressVideo(Context context, Uri sourceUri, CompressCallback callback) {
        compressVideo(context, sourceUri, 0, 0, callback);
    }

    public static void compressVideo(Context context, Uri sourceUri, long startPositionMs, long endPositionMs, CompressCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                File storageDir = new File(context.getFilesDir(), "videos");
                if (!storageDir.exists() && !storageDir.mkdirs()) {
                    mainHandler.post(() -> callback.onError("Failed to create video storage directory"));
                    return;
                }

                File destinationFile = new File(storageDir, "VID_comp_" + UUID.randomUUID() + ".mp4");

                File tempInput = new File(context.getCacheDir(), "temp_input_" + System.currentTimeMillis() + ".mp4");
                try (InputStream is = context.getContentResolver().openInputStream(sourceUri);
                     FileOutputStream os = new FileOutputStream(tempInput)) {
                    if (is != null) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        os.flush();
                    }
                }

                if (!tempInput.exists() || tempInput.length() == 0) {
                    mainHandler.post(() -> callback.onError("Invalid source video file"));
                    return;
                }

                // If input file is already small (<= 3.8 MB) and no clipping required, use directly
                if (startPositionMs == 0 && endPositionMs == 0 && tempInput.length() <= 3800 * 1024) {
                    if (tempInput.renameTo(destinationFile)) {
                        mainHandler.post(() -> callback.onSuccess(destinationFile));
                        return;
                    }
                }

                TransformationRequest transformationRequest = new TransformationRequest.Builder()
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .build();

                Transformer transformer = new Transformer.Builder(context)
                        .setTransformationRequest(transformationRequest)
                        .addListener(new Transformer.Listener() {
                            @Override
                            public void onCompleted(Composition composition, ExportResult exportResult) {
                                if (tempInput.exists() && !tempInput.delete()) {
                                    android.util.Log.w("VideoCompressor", "Could not delete temp input");
                                }
                                if (destinationFile.exists() && destinationFile.length() > 0) {
                                    mainHandler.post(() -> callback.onSuccess(destinationFile));
                                } else {
                                    mainHandler.post(() -> callback.onError("Compression produced empty video"));
                                }
                            }

                            @Override
                            public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                                if (tempInput.exists() && tempInput.length() <= 24 * 1024 * 1024) {
                                    if (!tempInput.renameTo(destinationFile)) {
                                        mainHandler.post(() -> callback.onError("Failed to move video file"));
                                        return;
                                    }
                                    mainHandler.post(() -> callback.onSuccess(destinationFile));
                                } else {
                                    if (tempInput.exists() && !tempInput.delete()) {
                                        android.util.Log.w("VideoCompressor", "Could not delete temp input");
                                    }
                                    mainHandler.post(() -> callback.onError("Video compression failed: " + ErrorParser.parseThrowable(exportException)));
                                }
                            }
                        })
                        .build();

                MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(Uri.fromFile(tempInput));
                if (startPositionMs > 0 || endPositionMs > 0) {
                    MediaItem.ClippingConfiguration.Builder clipBuilder = new MediaItem.ClippingConfiguration.Builder();
                    if (startPositionMs > 0) clipBuilder.setStartPositionMs(startPositionMs);
                    if (endPositionMs > startPositionMs) clipBuilder.setEndPositionMs(endPositionMs);
                    mediaItemBuilder.setClippingConfiguration(clipBuilder.build());
                }

                EditedMediaItem editedMediaItem = new EditedMediaItem.Builder(mediaItemBuilder.build()).build();
                mainHandler.post(() -> {
                    try {
                        transformer.start(editedMediaItem, destinationFile.getAbsolutePath());
                    } catch (Exception e) {
                        if (tempInput.exists() && tempInput.length() <= 24 * 1024 * 1024) {
                            if (tempInput.renameTo(destinationFile)) {
                                callback.onSuccess(destinationFile);
                            } else {
                                callback.onError("Failed to move fallback video");
                            }
                        } else {
                            callback.onError("Failed to start video compressor: " + ErrorParser.parseThrowable(e));
                        }
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Failed to compress video: " + ErrorParser.parseThrowable(e)));
            }
        }).start();
    }
}
