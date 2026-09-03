package com.voiceyanga.citizen.feature.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.core.network.ApiConstants;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ItemComplaintBinding;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Adapter for the main complaint feed.
 * [Rule 24] Visual representation of sync states.
 */
public class ComplaintAdapter extends ListAdapter<Complaint, ComplaintAdapter.ViewHolder> {

    private final OnComplaintClickListener listener;
    private String currentUserEmail;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint, android.view.View sharedElement);
        default void onSupportClick(Complaint complaint) {}
        default void onShareClick(Complaint complaint) {}
        default void onRetryClick(Complaint complaint) {}
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
                        oldItem.getCommentCount() == newItem.getCommentCount() &&
                        Objects.equals(oldItem.getFirstPhotoUri(), newItem.getFirstPhotoUri()) &&
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
            binding.tvTitle.setText(complaint.getTitle());
            binding.tvDescriptionSnippet.setText(complaint.getDescription());
            
            Context context = itemView.getContext();

            // Dynamic Branding based on Status/Priority
            String status = complaint.getStatus() != null ? complaint.getStatus() : "SUBMITTED";
            String priority = complaint.getCalculatedPriority();
            
            if ("RESOLVED".equals(status)) {
                binding.getRoot().setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_resolved_bg));
                binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_resolved_text)));
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                binding.getRoot().setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
                binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.neutral_100)));
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_green));
            }

            if ("CRITICAL".equals(priority)) {
                binding.getRoot().setStrokeColor(ContextCompat.getColor(context, R.color.primary_red));
                binding.getRoot().setStrokeWidth(4);
            } else {
                binding.getRoot().setStrokeColor(ContextCompat.getColor(context, R.color.neutral_200));
                binding.getRoot().setStrokeWidth(2);
            }

            // Sub-header: CATEGORY • TIME • LOCATION
            long diff = System.currentTimeMillis() - complaint.getCreatedAt();
            String timeStr;
            if (diff < 60000) timeStr = "Just now";
            else if (diff < 3600000) timeStr = (diff / 60000) + "m ago";
            else if (diff < 86400000) timeStr = (diff / 3600000) + "h ago";
            else timeStr = (diff / 86400000) + "d ago";

            String cat = complaint.getCategory() != null ? complaint.getCategory().toUpperCase() : "GENERAL";
            String loc = complaint.getLocation() != null ? complaint.getLocation() : "Unknown";
            
            // Priority handling: Only show if HIGH or CRITICAL
            String priorityText = "";
            if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) {
                priorityText = " • " + priority;
            }
            
            String subText = String.format("%s%s • %s • %s", cat, priorityText, timeStr, loc);
            
            if (complaint.getAssignedTo() != null && !complaint.getAssignedTo().isEmpty()) {
                subText += String.format(" • Assigned to %s", complaint.getAssignedTo());
            }
            binding.tvHeaderSub.setText(subText);
            
            // Photo Preview with Glide
            String photoUri = complaint.getFirstPhotoUri();
            
            if (photoUri != null && !photoUri.isEmpty()) {
                binding.flPhotoContainer.setVisibility(android.view.View.VISIBLE);
                
                String fullUrl;
                if (photoUri.startsWith("http") || photoUri.startsWith("content://") || photoUri.startsWith("file://")) {
                    fullUrl = photoUri;
                } else if (photoUri.startsWith("/data/") || photoUri.startsWith("/storage/") || photoUri.startsWith("/emulated/")) {
                    fullUrl = photoUri;
                } else {
                    fullUrl = ApiConstants.API_HOST + (photoUri.startsWith("/") ? "" : "/") + photoUri;
                }

                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(fullUrl)
                        .placeholder(R.color.neutral_200)
                        .error(R.drawable.vote_icon)
                        .centerCrop()
                        .into(binding.ivComplaintPreview);
                
                binding.ivComplaintPreview.setTransitionName("image_" + complaint.getClientUuid());
            } else {
                // If no photo, show a placeholder icon instead of hiding to maintain Facebook standard
                binding.flPhotoContainer.setVisibility(android.view.View.VISIBLE);
                binding.ivComplaintPreview.setImageResource(R.drawable.vote_icon);
                binding.ivComplaintPreview.setAlpha(0.1f);
            }

            // Engagement Counts (Facebook Style)
            int supports = complaint.getSupportCount();
            int comments = complaint.getCommentCount();
            
            if (supports > 0 || comments > 0) {
                binding.tvSupportCount.setVisibility(android.view.View.VISIBLE);
                String supportText = supports == 1 ? "1 support" : String.format(Locale.getDefault(), "%d supports", supports);
                
                if (comments > 0) {
                    String commentText = comments == 1 ? "1 comment" : String.format(Locale.getDefault(), "%d comments", comments);
                    binding.tvSupportCount.setText(String.format("%s • %s", supportText, commentText));
                } else {
                    binding.tvSupportCount.setText(supportText);
                }
            } else {
                binding.tvSupportCount.setText("Be the first to support this");
            }
            binding.tvCommentCount.setVisibility(android.view.View.GONE);

            // Status Badge
            binding.tvStatus.setText(complaint.getStatus());
            
            // Sync/Reference Footer
            String syncStatus = complaint.getSyncStatus();
            if ("SYNCED".equals(syncStatus)) {
                binding.tvSyncStatus.setText(complaint.getReferenceCode());
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.neutral_500));
            } else if ("FAILED".equals(syncStatus)) {
                binding.tvSyncStatus.setText(R.string.sync_failed_hint);
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_red));
            } else if ("SYNCING".equals(syncStatus)) {
                binding.tvSyncStatus.setText(R.string.syncing_hint);
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_green));
            } else {
                binding.tvSyncStatus.setText(R.string.pending_sync_hint);
                binding.tvSyncStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.neutral_500));
            }

            binding.tvSyncStatus.setOnClickListener(v -> {
                if (listener != null) {
                    if ("FAILED".equals(syncStatus)) {
                        com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
                        listener.onRetryClick(complaint);
                    }
                }
            });

            // User Badge logic: YOURS or SUPPORTED
            boolean isMyComplaint = currentUserEmail != null && currentUserEmail.equals(complaint.getAuthorEmail());
            
            if (isMyComplaint) {
                binding.tvUserBadge.setVisibility(android.view.View.VISIBLE);
                binding.tvUserBadge.setText("YOURS");
                binding.tvUserBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary_green_light)));
                binding.tvUserBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_green));
                
                binding.btnSupport.setEnabled(false);
                binding.btnSupport.setAlpha(0.5f);
            } else if (complaint.isSupportedByMe()) {
                binding.tvUserBadge.setVisibility(android.view.View.VISIBLE);
                binding.tvUserBadge.setText("SUPPORTED");
                binding.tvUserBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary_red_light)));
                binding.tvUserBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_red));
                
                binding.btnSupport.setEnabled(true);
                binding.btnSupport.setAlpha(1.0f);
                binding.btnSupport.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_red));
                binding.btnSupport.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary_red)));
            } else {
                binding.tvUserBadge.setVisibility(android.view.View.GONE);
                
                binding.btnSupport.setEnabled(true);
                binding.btnSupport.setAlpha(1.0f);
                binding.btnSupport.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.neutral_600));
                binding.btnSupport.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.neutral_600)));
            }

            binding.btnSupport.setOnClickListener(v -> {
                if (listener != null && !complaint.isSupportedByMe()) {
                    com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
                    listener.onSupportClick(complaint);
                }
            });

            binding.btnComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onComplaintClick(complaint, binding.ivComplaintPreview);
                }
            });

            binding.btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(complaint);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onComplaintClick(complaint, binding.ivComplaintPreview);
                }
            });
        }
    }
}
