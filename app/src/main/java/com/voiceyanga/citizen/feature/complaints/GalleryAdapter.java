package com.voiceyanga.citizen.feature.complaints;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemGalleryStoryBinding;

public class GalleryAdapter extends ListAdapter<Complaint, GalleryAdapter.ViewHolder> {

    public interface OnStoryClickListener {
        void onStoryClick(Complaint complaint);
    }

    private final OnStoryClickListener listener;

    public GalleryAdapter(OnStoryClickListener listener) {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getClientUuid().equals(newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                       java.util.Objects.equals(oldItem.getProofOfResolutionUri(), newItem.getProofOfResolutionUri()) &&
                       oldItem.getTitle().equals(newItem.getTitle());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemGalleryStoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemGalleryStoryBinding binding;
        private final OnStoryClickListener listener;

        ViewHolder(ItemGalleryStoryBinding binding, OnStoryClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Complaint complaint) {
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvLocation.setText(complaint.getLocation());
            binding.tvDescription.setText(complaint.getDescription());

            loadImage(complaint.getFirstPhotoUri(), binding.ivBefore);
            loadImage(complaint.getProofOfResolutionUri(), binding.ivAfter);
            
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onStoryClick(complaint);
            });
        }

        private void loadImage(String url, android.widget.ImageView imageView) {
            if (url != null && !url.isEmpty()) {
                String fullUrl = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(url);
                Glide.with(imageView)
                        .load(fullUrl)
                        .placeholder(R.color.neutral_400)
                        .error(R.drawable.vote_icon)
                        .centerCrop()
                        .into(imageView);
            } else {
                imageView.setImageResource(R.color.neutral_300);
            }
        }
    }
}
