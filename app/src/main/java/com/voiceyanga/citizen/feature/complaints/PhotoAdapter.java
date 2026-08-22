package com.voiceyanga.citizen.feature.complaints;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.databinding.ItemPhotoBinding;
import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private final List<String> photoUris = new ArrayList<>();
    private final OnRemoveListener listener;

    public PhotoAdapter(OnRemoveListener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<String> photos) {
        photoUris.clear();
        photoUris.addAll(photos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhotoBinding binding = ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(photoUris.get(position));
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPhotoBinding binding;

        ViewHolder(ItemPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String uri) {
            Glide.with(itemView.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(binding.ivPhoto);

            binding.btnRemove.setVisibility(listener != null ? View.VISIBLE : View.GONE);
            binding.btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemove(uri);
                }
            });
        }
    }

    public interface OnRemoveListener {
        void onRemove(String uri);
    }
}
