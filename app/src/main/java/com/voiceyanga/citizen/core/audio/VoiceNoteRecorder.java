package com.voiceyanga.citizen.core.audio;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class VoiceNoteRecorder {
    private static final String TAG = "VoiceNoteRecorder";
    private static final int MAX_DURATION_MS = 120_000; // 2 minutes

    private final Context context;
    private MediaRecorder recorder;
    private File outputFile;
    private boolean isRecording = false;
    private long startTime;

    public VoiceNoteRecorder(Context context) {
        this.context = context.getApplicationContext();
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && recorder != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= MAX_DURATION_MS) {
                    stop();
                    if (listener != null) listener.onAutoStop();
                } else {
                    if (listener != null) {
                        listener.onTimerTick((int) (elapsed / 1000));
                        listener.onAmplitudeUpdate(recorder.getMaxAmplitude());
                    }
                    handler.postDelayed(this, 100); // Faster tick for visualizer
                }
            }
        }
    };

    public interface RecorderListener {
        void onTimerTick(int seconds);
        void onAmplitudeUpdate(int amplitude);
        void onAutoStop();
        void onError(String message);
    }

    private RecorderListener listener;

    public void setListener(RecorderListener listener) {
        this.listener = listener;
    }

    public void start(File file) {
        if (isRecording) return;
        this.outputFile = file;
        
        try {
            release(); // Ensure cleanup before start
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(context);
            } else {
                @SuppressWarnings("deprecation")
                MediaRecorder legacyRecorder = new MediaRecorder();
                recorder = legacyRecorder;
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(file.getAbsolutePath());

            recorder.prepare();
            recorder.start();
            isRecording = true;
            startTime = System.currentTimeMillis();
            handler.post(timerRunnable);
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to start recording", e);
            if (listener != null) listener.onError("Failed to start recording: " + e.getMessage());
            release();
        }
    }

    public void stop() {
        if (!isRecording || recorder == null) return;
        try {
            isRecording = false;
            handler.removeCallbacks(timerRunnable);
            recorder.stop();
        } catch (RuntimeException e) {
            Log.e(TAG, "stop() failed (usually recording was too short)", e);
            if (outputFile != null && outputFile.exists()) {
                boolean deleted = outputFile.delete();
                if (!deleted) Log.w(TAG, "Could not delete corrupted recording file");
            }
        } finally {
            release();
        }
    }

    public void release() {
        isRecording = false;
        handler.removeCallbacks(timerRunnable);
        try {
            if (recorder != null) {
                recorder.reset();
                recorder.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing MediaRecorder", e);
        } finally {
            recorder = null;
        }
    }
}
