package com.voiceyanga.citizen.feature.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemComplaintBinding;

import java.util.List;
import java.util.Set;

/**
 * Adapter for the main complaint feed.
 * [Rule 24] Visual representation of sync states.
 */
public class ComplaintAdapter extends ListAdapter<Complaint, ComplaintAdapter.ViewHolder> {

    private final OnComplaintClickListener listener;
    private String currentUserEmail;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
        default void onSupportClick(Complaint complaint) {}
    }

    public ComplaintAdapter(OnComplaintClickListener listener) {
        this(listener, null);
    }

    public ComplaintAdapter(OnComplaintClickListener listener, String currentUserEmail) {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getClientUuid().equals(newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getSyncStatus().equals(newItem.getSyncStatus()) &&
                        oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getTitle().equals(newItem.getTitle()) &&
                        oldItem.getSupportCount() == newItem.getSupportCount() &&
                        oldItem.isSupportedByMe() == newItem.isSupportedByMe();
            }
        });
        this.listener = listener;
        this.currentUserEmail = currentUserEmail;
    }

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemComplaintBinding binding = ItemComplaintBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), currentUserEmail);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemComplaintBinding binding;
        private final OnComplaintClickListener listener;

        ViewHolder(ItemComplaintBinding binding, OnComplaintClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Complaint complaint, String currentUserEmail) {
            binding.tvCategory.setText(complaint.getCategory() != null ? complaint.getCategory().toUpperCase() : "");
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvLocation.setText(complaint.getLocation());
            
            // Relative time logic
            long diff = System.currentTimeMillis() - complaint.getCreatedAt();
            String timeStr;
            if (diff < 60000) timeStr = "Just now";
            else if (diff < 3600000) timeStr = (diff / 60000) + "m ago";
            else if (diff < 86400000) timeStr = (diff / 3600000) + "h ago";
            else timeStr = (diff / 86400000) + "d ago";
            binding.tvTime.setText(timeStr);
            
            String priority = complaint.getCalculatedPriority();
            binding.tvStatus.setText(itemView.getContext().getString(R.string.status_priority_format, priority, complaint.getStatus()));
            
            if ("CRITICAL".equals(priority)) {
                binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_red));
            } else if ("HIGH".equals(priority)) {
                binding.tvStatus.setTextColor(Color.parseColor("#E67E22")); // Orange
            } else {
                binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_green));
            }

            String syncStatus = complaint.getSyncStatus();
            int color;

            if ("SYNCED".equals(syncStatus)) {
                binding.tvSyncStatus.setText(complaint.getReferenceCode());
                color = ContextCompat.getColor(itemView.getContext(), R.color.neutral_500);
            } else if ("FAILED".equals(syncStatus)) {
                binding.tvSyncStatus.setText(R.string.sync_failed_hint);
                color = ContextCompat.getColor(itemView.getContext(), R.color.primary_red);
            } else if ("SYNCING".equals(syncStatus)) {
                binding.tvSyncStatus.setText(R.string.syncing_hint);
                color = ContextCompat.getColor(itemView.getContext(), R.color.primary_green);
            } else {
                binding.tvSyncStatus.setText(R.string.pending_sync_hint);
                color = ContextCompat.getColor(itemView.getContext(), R.color.neutral_400);
            }
            
            binding.tvSyncStatus.setTextColor(color);

            // Support button logic
            boolean isMyComplaint = currentUserEmail != null && currentUserEmail.equals(complaint.getAuthorEmail());
            
            if (isMyComplaint) {
                binding.btnSupport.setVisibility(android.view.View.GONE);
            } else {
                binding.btnSupport.setVisibility(android.view.View.VISIBLE);
                if (complaint.isSupportedByMe()) {
                    binding.btnSupport.setEnabled(false);
                    binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_red)));
                    binding.btnSupport.setTextColor(Color.WHITE);
                    binding.btnSupport.setIconTint(ColorStateList.valueOf(Color.WHITE));
                    binding.btnSupport.setStrokeWidth(0);
                } else {
                    binding.btnSupport.setEnabled(true);
                    binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_green)));
                    binding.btnSupport.setTextColor(Color.WHITE);
                    binding.btnSupport.setIconTint(ColorStateList.valueOf(Color.WHITE));
                    binding.btnSupport.setStrokeWidth(0);
                }
            }

            binding.btnSupport.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION && !complaint.isSupportedByMe()) {
                    com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
                    listener.onSupportClick(complaint);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onComplaintClick(complaint);
                }
            });
        }
    }
}
