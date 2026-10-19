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
    private final com.voiceyanga.citizen.data.repository.ComplaintRepository repository;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    public TrendingAdapter(OnComplaintClickListener listener, com.voiceyanga.citizen.data.repository.ComplaintRepository repository) {
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
        this.repository = repository;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTrendingComplaintBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), repository);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrendingComplaintBinding binding;

        ViewHolder(ItemTrendingComplaintBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Complaint complaint, com.voiceyanga.citizen.data.repository.ComplaintRepository repository) {
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvSupportCount.setText(binding.getRoot().getContext()
                    .getString(R.string.support_count_format_simple, complaint.getSupportCount()));

            String videoUrl = complaint.getVideoUrl();
            String videoPath = complaint.getVideoLocalPath();
            String videoThumbnail = complaint.getVideoThumbnailUrl();
            boolean hasVideo = (videoUrl != null && !videoUrl.isEmpty()) || (videoPath != null && !videoPath.isEmpty());

            String photoUri = complaint.getFirstPhotoUri();
            String mediaId = complaint.getFirstPhotoMediaId();

            if (hasVideo) {
                binding.flImageContainer.setVisibility(android.view.View.VISIBLE);
                binding.ivVideoPlayIndicator.setVisibility(android.view.View.VISIBLE);
                String targetMedia;
                if (videoThumbnail != null && !videoThumbnail.isEmpty()) {
                    targetMedia = videoThumbnail;
                } else if (videoUrl != null && !videoUrl.isEmpty()) {
                    targetMedia = videoUrl;
                } else {
                    targetMedia = videoPath;
                }
                String resolvedMedia = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(targetMedia);
                Glide.with(binding.ivPhoto.getContext())
                        .load(resolvedMedia)
                        .placeholder(R.color.primary_green_light)
                        .error(R.color.primary_green_light)
                        .into(binding.ivPhoto);
            } else if (photoUri != null && !photoUri.isEmpty()) {
                binding.flImageContainer.setVisibility(android.view.View.VISIBLE);
                binding.ivVideoPlayIndicator.setVisibility(android.view.View.GONE);
                com.voiceyanga.citizen.core.utils.PhotoLoader.loadPhoto(
                        complaint, photoUri, mediaId, repository, binding.ivPhoto, R.color.neutral_400);
            } else {
                binding.flImageContainer.setVisibility(android.view.View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onComplaintClick(complaint));
        }
    }
}
