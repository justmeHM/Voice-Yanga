package com.voiceyanga.citizen.feature.complaints;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ItemTimelineBinding;
import java.util.List;

/**
 * Adapter for the complaint status timeline.
 * [Rule 10] Uses semantic colors for status points.
 */
public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private final List<StatusPoint> points;

    public TimelineAdapter(List<StatusPoint> points) {
        this.points = points;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimelineBinding binding = ItemTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean nextCompleted = (position + 1 < points.size()) && points.get(position + 1).isCompleted;
        holder.bind(points.get(position), position == 0, position == getItemCount() - 1, nextCompleted);
    }

    @Override
    public int getItemCount() {
        return points.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineBinding binding;

        ViewHolder(ItemTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(StatusPoint point, boolean isFirst, boolean isLast, boolean isNextCompleted) {
            binding.tvStatusName.setText(point.label);
            binding.tvStatusDate.setText(point.date);
            
            binding.vLineTop.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            binding.vLineBottom.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            Context context = binding.getRoot().getContext();
            int green = ContextCompat.getColor(context, R.color.primary_green);
            int neutral = ContextCompat.getColor(context, R.color.neutral_200);

            if (point.isCompleted) {
                binding.ivDot.setImageTintList(ColorStateList.valueOf(green));
                binding.tvStatusName.setTextColor(ContextCompat.getColor(context, R.color.neutral_900));
                binding.tvStatusName.setAlpha(1.0f);
                binding.vLineTop.setBackgroundColor(green);
            } else {
                binding.ivDot.setImageTintList(ColorStateList.valueOf(neutral));
                binding.tvStatusName.setTextColor(ContextCompat.getColor(context, R.color.neutral_500));
                binding.tvStatusName.setAlpha(0.6f);
                binding.vLineTop.setBackgroundColor(neutral);
            }

            // The line leading to the NEXT point is green only if the NEXT point is completed
            binding.vLineBottom.setBackgroundColor(isNextCompleted ? green : neutral);
        }
    }

    public static class StatusPoint {
        String label;
        String date;
        boolean isCompleted;

        public StatusPoint(String label, String date, boolean isCompleted) {
            this.label = label;
            this.date = date;
            this.isCompleted = isCompleted;
        }
    }
}
