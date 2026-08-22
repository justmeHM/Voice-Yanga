package com.voiceyanga.citizen.feature.complaints;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.databinding.ItemTimelineBinding;
import java.util.List;

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
        holder.bind(points.get(position), position == 0, position == getItemCount() - 1);
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

        void bind(StatusPoint point, boolean isFirst, boolean isLast) {
            binding.tvStatusName.setText(point.label);
            binding.tvStatusDate.setText(point.date);
            
            binding.vLineTop.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            binding.vLineBottom.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

            if (point.isCompleted) {
                binding.ivDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#16834B")));
                binding.tvStatusName.setAlpha(1.0f);
            } else {
                binding.ivDot.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
                binding.tvStatusName.setAlpha(0.5f);
            }
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