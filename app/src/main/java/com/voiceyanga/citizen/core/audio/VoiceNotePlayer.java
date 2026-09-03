package com.voiceyanga.citizen.core.audio;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class VoiceNotePlayer {
    private static final String TAG = "VoiceNotePlayer";
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    public interface PlayerListener {
        void onCompletion();
        void onError(String message);
    }

    private PlayerListener listener;

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void play(String filePath) {
        try {
            stop();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                if (listener != null) listener.onCompletion();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                if (listener != null) listener.onError("Player error: " + what);
                release();
                return true;
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "play() failed", e);
            if (listener != null) listener.onError("Failed to play recording: " + e.getMessage());
            release();
        }
    }

    public void pause() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "pause() failed", e);
        }
    }

    public void resume() {
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                isPlaying = true;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "resume() failed", e);
        }
    }

    public void stop() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                release();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "stop() failed", e);
            release();
        } finally {
            isPlaying = false;
        }
    }

    public void release() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing MediaPlayer", e);
        } finally {
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
    
    public int getDuration() {
        try {
            return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }
}
