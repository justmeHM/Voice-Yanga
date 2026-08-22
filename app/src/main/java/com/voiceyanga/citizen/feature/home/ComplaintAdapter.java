package com.voiceyanga.citizen.feature.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemComplaintBinding;

/**
 * Adapter for the main complaint feed.
 * [Rule 24] Visual representation of sync states.
 */
public class ComplaintAdapter extends ListAdapter<Complaint, ComplaintAdapter.ViewHolder> {

    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    public ComplaintAdapter(OnComplaintClickListener listener) {
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
                        oldItem.getSupportCount() == newItem.getSupportCount();
            }
        });
        this.listener = listener;
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
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemComplaintBinding binding;
        private final OnComplaintClickListener listener;

        ViewHolder(ItemComplaintBinding binding, OnComplaintClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Complaint complaint) {
            binding.tvCategory.setText(complaint.getCategory() != null ? complaint.getCategory().toUpperCase() : "");
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvLocation.setText(complaint.getLocation());
            binding.tvStatus.setText(complaint.getStatus());
            
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

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onComplaintClick(complaint);
                }
            });
        }
    }
}
