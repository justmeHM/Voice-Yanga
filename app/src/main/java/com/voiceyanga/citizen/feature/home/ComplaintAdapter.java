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
import com.voiceyanga.citizen.BuildConfig;
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
    private final com.voiceyanga.citizen.data.repository.ComplaintRepository repository;
    private String currentUserEmail;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint, android.view.View sharedElement);
        default void onSupportClick(Complaint complaint) {}
        default void onShareClick(Complaint complaint) {}
        default void onRetryClick(Complaint complaint) {}
    }

    public ComplaintAdapter(OnComplaintClickListener listener, com.voiceyanga.citizen.data.repository.ComplaintRepository repository) {
        this(listener, repository, null);
    }

    public ComplaintAdapter(OnComplaintClickListener listener, com.voiceyanga.citizen.data.repository.ComplaintRepository repository, String currentUserEmail) {
        super(new DiffUtil.ItemCallback<Complaint>() {
            @Override
            public boolean areItemsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return Objects.equals(oldItem.getClientUuid(), newItem.getClientUuid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Complaint oldItem, @NonNull Complaint newItem) {
                return Objects.equals(oldItem.getSyncStatus(), newItem.getSyncStatus()) &&
                        Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                        Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                        oldItem.getSupportCount() == newItem.getSupportCount() &&
                        oldItem.getCommentCount() == newItem.getCommentCount() &&
                        Objects.equals(oldItem.getFirstPhotoUri(), newItem.getFirstPhotoUri()) &&
                        oldItem.isSupportedByMe() == newItem.isSupportedByMe();
            }
        });
        this.listener = listener;
        this.repository = repository;
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
        holder.bind(getItem(position), currentUserEmail, repository);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemComplaintBinding binding;
        private final OnComplaintClickListener listener;
        
        // Cache colors to reduce Context lookup in bind()
        private final int colorResolvedBg;
        private final int colorResolvedText;
        private final int colorWhite;
        private final int colorNeutral100;
        private final int colorPrimaryGreen;
        private final int colorPrimaryRed;
        private final int colorNeutral200;
        private final int colorNeutral500;
        private final int colorNeutral600;
        private final int colorPrimaryGreenLight;
        private final int colorPrimaryRedLight;

        ViewHolder(ItemComplaintBinding binding, OnComplaintClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            
            Context context = itemView.getContext();
            colorResolvedBg = ContextCompat.getColor(context, R.color.status_resolved_bg);
            colorResolvedText = ContextCompat.getColor(context, R.color.status_resolved_text);
            colorWhite = ContextCompat.getColor(context, R.color.white);
            colorNeutral100 = ContextCompat.getColor(context, R.color.neutral_100);
            colorPrimaryGreen = ContextCompat.getColor(context, R.color.primary_green);
            colorPrimaryRed = ContextCompat.getColor(context, R.color.primary_red);
            colorNeutral200 = ContextCompat.getColor(context, R.color.neutral_200);
            colorNeutral500 = ContextCompat.getColor(context, R.color.neutral_500);
            colorNeutral600 = ContextCompat.getColor(context, R.color.neutral_600);
            colorPrimaryGreenLight = ContextCompat.getColor(context, R.color.primary_green_light);
            colorPrimaryRedLight = ContextCompat.getColor(context, R.color.primary_red_light);
        }

        void bind(Complaint complaint, String currentUserEmail, com.voiceyanga.citizen.data.repository.ComplaintRepository repository) {
            String title = complaint.getTitle();
            String description = complaint.getDescription();
            binding.tvTitle.setText(title != null ? title : "");
            binding.tvDescriptionSnippet.setText(description != null ? description : "");
            
            // Set dynamic Initials placeholder based on category
            String categoryLabel = complaint.getCategory() != null ? complaint.getCategory().toUpperCase() : "GENERAL";
            if (categoryLabel.length() >= 2) {
                binding.ivUserAvatar.setText(categoryLabel.substring(0, 2));
            } else {
                binding.ivUserAvatar.setText(categoryLabel.isEmpty() ? "" : categoryLabel.substring(0, 1));
            }

            // Dynamic Branding based on Status/Priority
            String status = complaint.getStatus();
            if (status == null) status = "SUBMITTED";
            String priority = complaint.getCalculatedPriority();
            
            if ("RESOLVED".equals(status)) {
                binding.getRoot().setCardBackgroundColor(colorResolvedBg);
                binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(colorResolvedText));
                binding.tvStatus.setTextColor(colorWhite);
            } else {
                binding.getRoot().setCardBackgroundColor(colorWhite);
                binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(colorNeutral100));
                binding.tvStatus.setTextColor(colorPrimaryGreen);
            }

            if ("CRITICAL".equals(priority)) {
                binding.getRoot().setStrokeColor(colorPrimaryRed);
                binding.getRoot().setStrokeWidth(4);
            } else {
                binding.getRoot().setStrokeColor(colorNeutral200);
                binding.getRoot().setStrokeWidth(2);
            }

            // Sync status evaluation before building header sub-text
            String syncStatus = complaint.getSyncStatus();
            String refSuffix = "";
            if ("SYNCED".equals(syncStatus) && complaint.getReferenceCode() != null && !complaint.getReferenceCode().isEmpty()) {
                refSuffix = " • #" + complaint.getReferenceCode();
            } else if ("SYNCING".equals(syncStatus)) {
                refSuffix = " • Syncing...";
            } else if ("FAILED".equals(syncStatus)) {
                refSuffix = " • Sync Failed";
            }

            // Sub-header: TIME • LOCATION
            String timeStr = getRelativeTime(complaint.getCreatedAt());
            String loc = complaint.getLocation() != null ? complaint.getLocation() : "Unknown";
            
            // Priority handling: Only show if HIGH or CRITICAL
            String priorityText = "";
            if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) {
                priorityText = priority + " • ";
            }
            
            String subText = String.format("%s%s • %s%s", priorityText, timeStr, loc, refSuffix);
            
            if (complaint.getAssignedTo() != null && !complaint.getAssignedTo().isEmpty()) {
                subText += String.format(" • Assigned to %s", complaint.getAssignedTo());
            }
            binding.tvHeaderSub.setText(subText);
            
            // Photo Preview with PhotoLoader
            String photoUri = complaint.getFirstPhotoUri();
            String mediaId = complaint.getFirstPhotoMediaId();
            
            if (photoUri != null && !photoUri.isEmpty()) {
                binding.flPhotoContainer.setVisibility(android.view.View.VISIBLE);
                binding.ivComplaintPreview.setAlpha(1.0f); // Reset alpha for recycled views
                com.voiceyanga.citizen.core.utils.PhotoLoader.loadPhoto(
                        complaint, photoUri, mediaId, repository, binding.ivComplaintPreview, R.color.neutral_200);
                binding.ivComplaintPreview.setTransitionName("image_" + complaint.getClientUuid());
            } else {
                binding.flPhotoContainer.setVisibility(android.view.View.GONE);
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
            
            // Sync/Reference Footer layout backward compatibility click listener
            binding.tvSyncStatus.setOnClickListener(v -> {
                if (listener != null) {
                    if ("FAILED".equals(syncStatus)) {
                        com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
                        listener.onRetryClick(complaint);
                    }
                }
            });

            // User Badge logic: YOURS or SUPPORTED compact overlay indicator
            boolean isMyComplaint = currentUserEmail != null && currentUserEmail.equals(complaint.getAuthorEmail());
            
            if (isMyComplaint) {
                binding.tvUserBadge.setVisibility(android.view.View.VISIBLE);
                binding.tvUserBadge.setText("YOURS");
                binding.tvUserBadge.setBackgroundTintList(ColorStateList.valueOf(colorPrimaryGreenLight));
                binding.tvUserBadge.setTextColor(colorPrimaryGreen);
                binding.ivSupportedIndicator.setVisibility(android.view.View.GONE);
                
                binding.btnSupport.setEnabled(false);
                binding.btnSupport.setAlpha(0.5f);
            } else if (complaint.isSupportedByMe()) {
                binding.tvUserBadge.setVisibility(android.view.View.GONE);
                binding.ivSupportedIndicator.setVisibility(android.view.View.VISIBLE);
                
                binding.btnSupport.setEnabled(true);
                binding.btnSupport.setAlpha(1.0f);
                binding.btnSupport.setText(R.string.status_supported);
                binding.btnSupport.setTextColor(colorPrimaryRed);
                binding.btnSupport.setIconTint(ColorStateList.valueOf(colorPrimaryRed));
            } else {
                binding.tvUserBadge.setVisibility(android.view.View.GONE);
                binding.ivSupportedIndicator.setVisibility(android.view.View.GONE);
                
                binding.btnSupport.setEnabled(true);
                binding.btnSupport.setAlpha(1.0f);
                binding.btnSupport.setText(R.string.action_support);
                binding.btnSupport.setTextColor(colorNeutral600);
                binding.btnSupport.setIconTint(ColorStateList.valueOf(colorNeutral600));
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

        private String getRelativeTime(long createdAt) {
            long now = System.currentTimeMillis();
            long diff = now - createdAt;
            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + "m ago";
            if (diff < 86400000) return (diff / 3600000) + "h ago";
            return (diff / 86400000) + "d ago";
        }
    }
}
