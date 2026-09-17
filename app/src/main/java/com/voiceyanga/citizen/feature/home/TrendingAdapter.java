package com.voiceyanga.citizen.feature.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemTrendingComplaintBinding;

public class TrendingAdapter extends ListAdapter<Complaint, TrendingAdapter.ViewHolder> {

    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    public TrendingAdapter(OnComplaintClickListener listener) {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getClientUuid().equals(newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getSupportCount() == newItem.getSupportCount() &&
                       oldItem.getTitle().equals(newItem.getTitle());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTrendingComplaintBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrendingComplaintBinding binding;

        ViewHolder(ItemTrendingComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Complaint complaint) {
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvSupportCount.setText(binding.getRoot().getContext()
                    .getString(R.string.support_count_format_simple, complaint.getSupportCount()));

            String rawPath = complaint.getFirstPhotoUri();
            if (rawPath != null && !rawPath.isEmpty()) {
                String resolvedUrl = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(rawPath);
                
                android.util.Log.d("TrendingAdapter", "Loading photo: [" + resolvedUrl + "] for: " + complaint.getTitle());
                
                Glide.with(binding.ivPhoto.getContext())
                        .load(resolvedUrl)
                        .placeholder(R.color.neutral_400) // Distinct grey
                        .error(R.drawable.vote_icon)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(binding.ivPhoto);
            } else {
                android.util.Log.w("TrendingAdapter", "No photo URL for: " + complaint.getTitle());
                binding.ivPhoto.setImageResource(R.color.neutral_300);
            }

            binding.getRoot().setOnClickListener(v -> listener.onComplaintClick(complaint));
        }
    }
}
