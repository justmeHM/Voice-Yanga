package com.voiceyanga.citizen.feature.notifications;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ItemNotificationBinding;
import com.voiceyanga.citizen.data.local.entity.Notification;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the notification list.
 * [Rule 57] Uses string resources for date formatting.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;
        private final SimpleDateFormat dateFormat;

        ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.dateFormat = new SimpleDateFormat(
                    binding.getRoot().getContext().getString(R.string.notification_date_format), 
                    Locale.getDefault()
            );
        }

        void bind(Notification notification) {
            binding.tvTitle.setText(notification.getTitle());
            binding.tvMessage.setText(notification.getMessage());
            binding.tvDate.setText(dateFormat.format(new Date(notification.getTimestamp())));
            
            if (notification.isRead()) {
                binding.llNotificationRoot.setAlpha(0.6f);
            } else {
                binding.llNotificationRoot.setAlpha(1.0f);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
    }
}
