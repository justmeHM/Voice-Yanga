package com.voiceyanga.citizen.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.voiceyanga.citizen.R;
import java.util.ArrayList;
import java.util.List;

public class VisualizerView extends View {
    private static final int MAX_BARS = 30;
    private final List<Float> amplitudes = new ArrayList<>();
    private final Paint paint = new Paint();
    private float barWidth = 8f;
    private float barGap = 4f;

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(ContextCompat.getColor(context, R.color.primary_green));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public void addAmplitude(float amplitude) {
        // Normalize amplitude (MediaRecorder returns 0-32767)
        float normalized = Math.min(amplitude / 32767f, 1f);
        amplitudes.add(normalized);
        if (amplitudes.size() > MAX_BARS) {
            amplitudes.remove(0);
        }
        invalidate();
    }

    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (amplitudes.isEmpty()) return;

        float midY = getHeight() / 2f;
        float startX = getWidth() - (amplitudes.size() * (barWidth + barGap));

        for (int i = 0; i < amplitudes.size(); i++) {
            float amp = amplitudes.get(i);
            float barHeight = Math.max(8f, amp * getHeight() * 0.8f);
            
            float left = startX + i * (barWidth + barGap);
            float top = midY - barHeight / 2f;
            float right = left + barWidth;
            float bottom = midY + barHeight / 2f;

            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, paint);
        }
    }
}
