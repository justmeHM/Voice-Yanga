package com.voiceyanga.citizen.feature.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemComplaintBinding;

public class ComplaintAdapter extends ListAdapter<Complaint, ComplaintAdapter.ViewHolder> {

    public ComplaintAdapter() {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getClientUuid().equals(newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getSyncStatus().equals(newItem.getSyncStatus()) &&
                        oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getTitle().equals(newItem.getTitle());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemComplaintBinding binding = ItemComplaintBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemComplaintBinding binding;

        ViewHolder(ItemComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Complaint complaint) {
            binding.tvCategory.setText(complaint.getCategory() != null ? complaint.getCategory().toUpperCase() : "");
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvLocation.setText(complaint.getLocation());
            binding.tvStatus.setText(complaint.getStatus());
            
            String syncInfo = complaint.getSyncStatus();
            if ("SYNCED".equals(syncInfo) && complaint.getReferenceCode() != null) {
                syncInfo = complaint.getReferenceCode();
            }
            binding.tvSyncStatus.setText(syncInfo);
        }
    }
}