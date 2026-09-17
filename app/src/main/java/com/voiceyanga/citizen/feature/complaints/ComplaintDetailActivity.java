package com.voiceyanga.citizen.feature.complaints;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.voiceyanga.citizen.BuildConfig;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.databinding.ActivityComplaintDetailBinding;
import com.voiceyanga.citizen.data.remote.dto.HistoryItem;
import com.voiceyanga.citizen.data.repository.ComplaintRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.voiceyanga.citizen.core.audio.VoiceNotePlayer;
import com.voiceyanga.citizen.core.utils.MediaResolver;
import okhttp3.OkHttpClient;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ComplaintDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMPLAINT_UUID = "extra_complaint_uuid";

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @Inject
    OkHttpClient okHttpClient;

    private ActivityComplaintDetailBinding binding;
    private ComplaintDetailViewModel viewModel;
    private String complaintUuid;
    private PhotoAdapter photoAdapter;
    private CommentAdapter commentAdapter;
    private Complaint currentComplaint;
    private VoiceNotePlayer voiceNotePlayer;
    private java.util.List<HistoryItem> cachedHistory = null;
    private List<TimelineAdapter.StatusPoint> timelinePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        complaintUuid = getIntent().getStringExtra(EXTRA_COMPLAINT_UUID);
        if (complaintUuid == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ComplaintDetailViewModel.class);

        setupToolbar();
        setupPhotoList();
        setupCommentList();
        setupVoicePlayer();
        observeViewModel();
        setupListeners();
    }

    private void setupVoicePlayer() {
        voiceNotePlayer = new VoiceNotePlayer();
        voiceNotePlayer.setListener(new VoiceNotePlayer.PlayerListener() {
            @Override
            public void onCompletion() {
                binding.btnPlayVoice.setIconResource(R.drawable.ic_play);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ComplaintDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                binding.btnPlayVoice.setIconResource(R.drawable.ic_play);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (voiceNotePlayer != null) {
            voiceNotePlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceNotePlayer != null) {
            voiceNotePlayer.release();
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.inflateMenu(R.menu.complaint_detail_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                shareComplaint();
                return true;
            }
            return false;
        });
    }

    private void shareComplaint() {
        if (currentComplaint == null) return;
        
        String photoUri = currentComplaint.getFirstPhotoUri();
        String shareText = String.format("Help me get this issue noticed! %s in %s. Reported via Voice Yanga app.", 
                currentComplaint.getTitle(), currentComplaint.getLocation());

        if (photoUri != null && !photoUri.isEmpty()) {
            if (photoUri.startsWith("http")) {
                shareWithGlide(photoUri, shareText);
            } else {
                try {
                    File cachePath = new File(getCacheDir(), "images");
                    cachePath.mkdirs();
                    File shareFile = new File(cachePath, "share_image_detail.png");
                    
                    InputStream is;
                    if (photoUri.startsWith("content://")) {
                        is = getContentResolver().openInputStream(android.net.Uri.parse(photoUri));
                    } else {
                        is = new FileInputStream(photoUri);
                    }
                    
                    if (is != null) {
                        FileOutputStream os = new FileOutputStream(shareFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
                        is.close();
                        os.close();
                        
                        triggerShare(shareFile, shareText);
                    } else {
                        shareWithGlide(photoUri, shareText);
                    }
                } catch (Exception e) {
                    shareWithGlide(photoUri, shareText);
                }
            }
        } else {
            shareAsText(shareText);
        }
    }

    private void shareWithGlide(String url, String shareText) {
        String fullUrl = url;
        if (!url.startsWith("http") && !url.startsWith("content://") && !url.startsWith("file://") && !url.startsWith("/")) {
            fullUrl = BuildConfig.API_ORIGIN + "/" + url;
        }

        Glide.with(this)
            .asBitmap()
            .load(fullUrl)
            .into(new CustomTarget<android.graphics.Bitmap>() {
                @Override
                public void onResourceReady(@NonNull android.graphics.Bitmap resource, @Nullable Transition<? super android.graphics.Bitmap> transition) {
                    try {
                        File cachePath = new File(getCacheDir(), "images");
                        cachePath.mkdirs();
                        File imageFile = new File(cachePath, "share_image_detail.png");
                        FileOutputStream stream = new FileOutputStream(imageFile);
                        resource.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();
                        triggerShare(imageFile, shareText);
                    } catch (IOException e) {
                        shareAsText(shareText);
                    }
                }

                @Override
                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

                @Override
                public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                    shareAsText(shareText);
                }
            });
    }

    private void triggerShare(File imageFile, String text) {
        runOnUiThread(() -> {
            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share Report"));
        });
    }

    private void shareAsText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void setupPhotoList() {
        photoAdapter = new PhotoAdapter(null);
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupCommentList() {
        commentAdapter = new CommentAdapter();
        binding.rvComments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComments.setAdapter(commentAdapter);
    }

    private void observeViewModel() {
        viewModel.getComplaint(complaintUuid).observe(this, complaint -> {
            if (complaint != null) {
                this.currentComplaint = complaint;
                displayComplaint(complaint);
                viewModel.refreshHistory(complaintUuid);
                binding.layoutShimmer.getRoot().setVisibility(View.GONE);
                binding.scrollContent.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getHistory().observe(this, this::updateTimelineWithHistory);

        viewModel.getPhotos(complaintUuid).observe(this, photos -> {
            if (photos != null && !photos.isEmpty()) {
                binding.rvPhotos.setVisibility(View.VISIBLE);
                List<String> uris = photos.stream()
                        .map(ComplaintPhoto::getPhotoUri)
                        .collect(Collectors.toList());
                photoAdapter.setPhotos(uris);
            } else {
                binding.rvPhotos.setVisibility(View.GONE);
            }
        });

        viewModel.getComments(complaintUuid).observe(this, comments -> {
            if (comments != null) {
                commentAdapter.setComments(comments);
            }
        });

        viewModel.getCommentSuccess().observe(this, success -> {
            if (success != null && success) {
                binding.etComment.setText("");
            }
        });
    }

    private void displayComplaint(Complaint complaint) {
        binding.tvRefCode.setText(complaint.getReferenceCode() != null ? complaint.getReferenceCode() : "PENDING SYNC");
        binding.tvTitle.setText(complaint.getTitle());

        // Sync Retry & Edit Logic
        if ("FAILED".equals(complaint.getSyncStatus())) {
            binding.btnEditReport.setVisibility(View.VISIBLE);
            binding.btnRetrySync.setVisibility(View.VISIBLE);
            binding.btnRetrySync.setText(R.string.btn_retry_upload);
            binding.btnRetrySync.setEnabled(true);
            
            if (complaint.getFailureReason() != null) {
                binding.tvFailureReason.setVisibility(View.VISIBLE);
                binding.tvFailureReason.setText(complaint.getFailureReason());
            } else {
                binding.tvFailureReason.setVisibility(View.GONE);
            }
        } else if ("SYNCING".equals(complaint.getSyncStatus()) || "PENDING".equals(complaint.getSyncStatus())) {
            binding.btnEditReport.setVisibility(View.GONE);
            binding.btnRetrySync.setVisibility(View.VISIBLE);
            binding.btnRetrySync.setText("Retrying...");
            binding.btnRetrySync.setEnabled(false);
            binding.tvFailureReason.setVisibility(View.GONE);
        } else {
            binding.btnEditReport.setVisibility(View.GONE);
            binding.btnRetrySync.setVisibility(View.GONE);
            binding.tvFailureReason.setVisibility(View.GONE);
        }
        
        String cat = complaint.getCategory();
        binding.tvCategory.setText(cat != null ? cat.toUpperCase() : "GENERAL");
        
        binding.tvLocation.setText(complaint.getLocation() != null ? complaint.getLocation() : "Unknown");
        binding.tvDescription.setText(complaint.getDescription());
        
        String priority = complaint.getCalculatedPriority();
        if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) {
            binding.tvPriority.setVisibility(View.VISIBLE);
            binding.tvPriority.setText(String.format(getString(R.string.priority_format), priority));
            
            if ("CRITICAL".equals(priority)) {
                binding.tvPriority.setTextColor(ContextCompat.getColor(this, R.color.primary_red));
            } else {
                binding.tvPriority.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
            }
        } else {
            binding.tvPriority.setVisibility(View.GONE);
        }

        // Strategy 6: Milestone Badges
        int supportCount = complaint.getSupportCount();
        if (supportCount >= 50) {
            binding.tvMilestoneBadge.setVisibility(View.VISIBLE);
            binding.tvMilestoneBadge.setText("CRITICAL PRIORITY");
            binding.tvMilestoneBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_red)));
            binding.tvMilestoneBadge.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (supportCount >= 20) {
            binding.tvMilestoneBadge.setVisibility(View.VISIBLE);
            binding.tvMilestoneBadge.setText("COMMUNITY PRIORITY");
            binding.tvMilestoneBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_green)));
            binding.tvMilestoneBadge.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (supportCount >= 10) {
            binding.tvMilestoneBadge.setVisibility(View.VISIBLE);
            binding.tvMilestoneBadge.setText("TRENDING");
            binding.tvMilestoneBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_green_light)));
            binding.tvMilestoneBadge.setTextColor(ContextCompat.getColor(this, R.color.primary_green));
        } else {
            binding.tvMilestoneBadge.setVisibility(View.GONE);
        }
        
        binding.btnSupport.setText(String.format(Locale.getDefault(), getString(R.string.support_count_format), complaint.getSupportCount()));

        if (complaint.isSupportedByMe()) {
            binding.btnSupport.setEnabled(false);
            binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_red)));
            binding.btnSupport.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.btnSupport.setIconTintResource(R.color.white);
        } else {
            binding.btnSupport.setEnabled(true);
            binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.white)));
            binding.btnSupport.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            binding.btnSupport.setIconTintResource(R.color.primary_green);
        }

        updateTimeline(complaint);
        setupVoiceNote(complaint);
        setupProofOfResolution(complaint);
    }

    private void setupProofOfResolution(Complaint complaint) {
        String proofUri = complaint.getProofOfResolutionUri();
        if ("RESOLVED".equals(complaint.getStatus()) && proofUri != null && !proofUri.isEmpty()) {
            binding.cardProof.setVisibility(View.VISIBLE);
            String fullUrl = com.voiceyanga.citizen.core.utils.MediaUtils.resolvePhotoUrl(proofUri);
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.color.neutral_400)
                    .into(binding.ivProof);
        } else {
            binding.cardProof.setVisibility(View.GONE);
        }
    }

    private void setupVoiceNote(Complaint complaint) {
        String voiceUrl = complaint.getVoiceNoteUrl();
        String localPath = complaint.getVoiceNoteLocalPath();
        
        if ((voiceUrl != null && !voiceUrl.isEmpty()) || (localPath != null && new File(localPath).exists())) {
            binding.cardVoiceNote.setVisibility(View.VISIBLE);
            int duration = complaint.getVoiceNoteDuration();
            binding.tvVoiceDuration.setText(String.format(Locale.getDefault(), "%d:%02d", duration / 60, duration % 60));

            binding.btnPlayVoice.setOnClickListener(v -> {
                if (voiceNotePlayer.isPlaying()) {
                    voiceNotePlayer.pause();
                    binding.btnPlayVoice.setIconResource(R.drawable.ic_play);
                } else {
                    playVoiceNote(complaint);
                }
            });
        } else {
            binding.cardVoiceNote.setVisibility(View.GONE);
        }
    }

    private void playVoiceNote(Complaint complaint) {
        String voiceUrl = complaint.getVoiceNoteUrl();
        String localPath = complaint.getVoiceNoteLocalPath();

        if (localPath != null && new File(localPath).exists()) {
            voiceNotePlayer.play(localPath);
            binding.btnPlayVoice.setIconResource(R.drawable.ic_pause);
        } else if (voiceUrl != null) {
            binding.btnPlayVoice.setEnabled(false);
            new Thread(() -> {
                try {
                    String resolvedUrl = MediaResolver.resolveMediaUrl(voiceUrl, sessionManager, okHttpClient);
                    runOnUiThread(() -> {
                        binding.btnPlayVoice.setEnabled(true);
                        voiceNotePlayer.play(resolvedUrl);
                        binding.btnPlayVoice.setIconResource(R.drawable.ic_pause);
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        binding.btnPlayVoice.setEnabled(true);
                        Toast.makeText(this, "Failed to resolve recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }

    private void updateTimeline(Complaint complaint) {
        updateTimelineWithHistory(this.cachedHistory);
    }

    private void updateTimelineWithHistory(List<HistoryItem> history) {
        if (history != null) {
            this.cachedHistory = history;
        }
        if (currentComplaint == null) return;
        
        List<TimelineAdapter.StatusPoint> points = new ArrayList<>();
        String currentStatus = currentComplaint.getStatus() != null ? currentComplaint.getStatus() : "SUBMITTED";
        String pending = getString(R.string.status_pending);
        
        // Define statuses in order
        List<String> statuses = Arrays.asList("SUBMITTED", "REVIEWED", "VERIFIED", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED");
        
        int currentStatusIndex = statuses.indexOf(currentStatus);

        for (int i = 0; i < statuses.size(); i++) {
            String status = statuses.get(i);
            String label = getStatusLabel(status);
            String date = pending;
            boolean completed = i <= currentStatusIndex && currentStatusIndex != -1;
            String subLabel = null;

            // 1. Resolve date and subLabel from history
            if (history != null) {
                for (HistoryItem item : history) {
                    if (status.equals(item.status)) {
                        date = formatDate(item.createdAt);
                        completed = true;
                        break;
                    }
                }
            }

            // 2. Fallback for SUBMITTED date if history missing
            if (i == 0 && date.equals(pending)) {
                date = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(currentComplaint.getCreatedAt()));
            }

            // 3. Fallback for COMPLETED status dates if history missing
            if (completed && date.equals(pending)) {
                date = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(currentComplaint.getUpdatedAt()));
            }

            // 4. AUTO-CLOSE Logic: If RESOLVED is reached, CLOSED is also marked completed
            if ("CLOSED".equals(status) && currentStatusIndex >= statuses.indexOf("RESOLVED")) {
                completed = true;
                if (date.equals(pending)) {
                    // Inherit date from Resolved if possible, otherwise use updatedAt
                    date = getPointDate(points, getString(R.string.status_resolved));
                    if (date.equals(pending)) {
                        date = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(currentComplaint.getUpdatedAt()));
                    }
                }
            }
            
            // 5. Assignment data display
            if ("ASSIGNED".equals(status) && currentComplaint.getAssignedTo() != null) {
                subLabel = currentComplaint.getAssignedTo();
                if (currentComplaint.getAssignedOrganization() != null) {
                    subLabel += " (" + currentComplaint.getAssignedOrganization() + ")";
                }
            }

            points.add(new TimelineAdapter.StatusPoint(label, date, completed, subLabel));
        }

        if ("REJECTED".equals(currentStatus)) {
            points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_rejected), formatDate(null), true));
        }

        this.timelinePoints = points;
    }

    private String getPointDate(List<TimelineAdapter.StatusPoint> points, String label) {
        for (TimelineAdapter.StatusPoint p : points) {
            if (p.label.equals(label)) return p.date;
        }
        return new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date());
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "SUBMITTED": return getString(R.string.status_submitted);
            case "REVIEWED": return getString(R.string.status_reviewed);
            case "VERIFIED": return "Verified";
            case "ASSIGNED": return getString(R.string.status_assigned);
            case "IN_PROGRESS": return getString(R.string.status_in_progress);
            case "RESOLVED": return getString(R.string.status_resolved);
            case "CLOSED": return "Closed";
            case "REJECTED": return getString(R.string.status_rejected);
            default: return status;
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date());
        
        long ts = ComplaintRepository.parseServerDate(isoDate);
        return new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(ts));
    }

    private void setupListeners() {
        binding.btnEditReport.setOnClickListener(v -> {
            com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
            Intent intent = new Intent(this, CreateComplaintActivity.class);
            intent.putExtra("extra_complaint_uuid", complaintUuid);
            startActivity(intent);
        });

        binding.btnRetrySync.setOnClickListener(v -> {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.Network network = cm.getActiveNetwork();
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            boolean isConnected = caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            
            if (!isConnected) {
                Toast.makeText(this, "Please check your internet connection and try again.", Toast.LENGTH_LONG).show();
                return;
            }

            com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
            viewModel.retryComplaint(complaintUuid);
            
            binding.btnRetrySync.setText("Initiating retry...");
            binding.btnRetrySync.setEnabled(false);
        });

        binding.btnSupport.setOnClickListener(v -> {
            com.voiceyanga.citizen.core.utils.HapticHelper.performSuccess(v);
            viewModel.supportComplaint(complaintUuid);
            
            binding.btnSupport.setEnabled(false);
            binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_red)));
            binding.btnSupport.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.btnSupport.setIconTintResource(R.color.white);
            
            Toast.makeText(this, R.string.support_thanks, Toast.LENGTH_SHORT).show();
        });

        binding.btnPostComment.setOnClickListener(v -> {
            String message = binding.etComment.getText().toString().trim();
            if (!message.isEmpty()) {
                com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
                viewModel.postComment(complaintUuid, message);
            }
        });

        binding.fabTimeline.setOnClickListener(v -> {
            com.voiceyanga.citizen.core.utils.HapticHelper.performClick(v);
            showTimelineBottomSheet();
        });
    }

    private void showTimelineBottomSheet() {
        if (timelinePoints == null || timelinePoints.isEmpty()) {
            Toast.makeText(this, "Resolution timeline not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.Theme_VoiceYanga);
        View view = getLayoutInflater().inflate(R.layout.layout_timeline_bottom_sheet, null);
        
        RecyclerView rv = view.findViewById(R.id.rvTimelineSheet);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new TimelineAdapter(timelinePoints));
        
        dialog.setContentView(view);
        dialog.show();
    }
}
