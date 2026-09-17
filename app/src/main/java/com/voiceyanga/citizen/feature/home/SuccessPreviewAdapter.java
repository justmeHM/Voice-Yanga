package com.voiceyanga.citizen.feature.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.utils.MediaUtils;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemSuccessPreviewBinding;

public class SuccessPreviewAdapter extends ListAdapter<Complaint, SuccessPreviewAdapter.ViewHolder> {

    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    public SuccessPreviewAdapter(OnComplaintClickListener listener) {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getClientUuid().equals(newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return oldItem.getTitle().equals(newItem.getTitle()) &&
                       oldItem.getProofOfResolutionUri() != null &&
                       oldItem.getProofOfResolutionUri().equals(newItem.getProofOfResolutionUri());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSuccessPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSuccessPreviewBinding binding;

        ViewHolder(ItemSuccessPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Complaint complaint) {
            binding.tvTitle.setText(complaint.getTitle());
            
            String photoPath = complaint.getProofOfResolutionUri();
            if (photoPath == null || photoPath.isEmpty()) photoPath = complaint.getFirstPhotoUri();

            String resolvedUrl = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(photoPath);
            
            Glide.with(binding.ivAfter.getContext())
                    .load(resolvedUrl)
                    .placeholder(R.color.neutral_400)
                    .error(R.drawable.vote_icon)
                    .centerCrop()
                    .into(binding.ivAfter);

            binding.getRoot().setOnClickListener(v -> listener.onComplaintClick(complaint));
        }
    }
}
