package com.voiceyanga.citizen.feature.complaints;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.databinding.ItemPhotoBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private final List<String> photoUris = new ArrayList<>();
    private final Map<String, String> photoLabels = new HashMap<>();
    private final OnRemoveListener listener;
    
    private static final String[] QUICK_LABELS = {
            "General", "Close-up", "Wide View", "Damage Detail", "Location Marker"
    };

    public interface OnRemoveListener {
        void onRemove(String uri);
        default void onPhotoClick(String uri) {}
        default void onLabelChanged(String uri, String label) {}
    }

    public PhotoAdapter(OnRemoveListener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<String> photos) {
        photoUris.clear();
        photoUris.addAll(photos);
        notifyDataSetChanged();
    }

    public String getLabel(String uri) {
        return photoLabels.getOrDefault(uri, "General");
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
            String fullUrl = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(uri);

            Glide.with(itemView.getContext())
                    .load(fullUrl)
                    .centerCrop()
                    .into(binding.ivPhoto);

            String label = photoLabels.getOrDefault(uri, "General");
            binding.tvLabel.setText(label);
            binding.tvLabel.setVisibility(View.VISIBLE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(uri);
                }
            });

            binding.tvLabel.setOnClickListener(v -> {
                if (listener != null) {
                    String currentLabel = photoLabels.getOrDefault(uri, "General");
                    String nextLabel = QUICK_LABELS[0];
                    
                    for (int i = 0; i < QUICK_LABELS.length; i++) {
                        if (QUICK_LABELS[i].equals(currentLabel)) {
                            nextLabel = QUICK_LABELS[(i + 1) % QUICK_LABELS.length];
                            break;
                        }
                    }
                    
                    photoLabels.put(uri, nextLabel);
                    listener.onLabelChanged(uri, nextLabel);
                    notifyItemChanged(getBindingAdapterPosition());
                }
            });

            binding.btnRemove.setVisibility(listener != null ? View.VISIBLE : View.GONE);
            binding.btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemove(uri);
                }
            });
        }
    }
}
