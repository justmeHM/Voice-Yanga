package com.voiceyanga.citizen.feature.complaints;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.data.local.entity.ComplaintPhoto;
import com.voiceyanga.citizen.databinding.ActivityComplaintDetailBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ComplaintDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMPLAINT_UUID = "extra_complaint_uuid";

    private ActivityComplaintDetailBinding binding;
    private ComplaintDetailViewModel viewModel;
    private String complaintUuid;
    private PhotoAdapter photoAdapter;
    private CommentAdapter commentAdapter;
    private Complaint currentComplaint;

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
        setupTimeline();
        setupPhotoList();
        setupCommentList();
        observeViewModel();
        setupListeners();
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
                    java.io.File cachePath = new java.io.File(getCacheDir(), "images");
                    cachePath.mkdirs();
                    java.io.File shareFile = new java.io.File(cachePath, "share_image_detail.png");
                    
                    java.io.InputStream is;
                    if (photoUri.startsWith("content://")) {
                        is = getContentResolver().openInputStream(android.net.Uri.parse(photoUri));
                    } else {
                        is = new java.io.FileInputStream(photoUri);
                    }
                    
                    if (is != null) {
                        java.io.FileOutputStream os = new java.io.FileOutputStream(shareFile);
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
            fullUrl = com.voiceyanga.citizen.core.network.ApiConstants.API_HOST + "/" + url;
        }

        com.bumptech.glide.Glide.with(this)
            .asBitmap()
            .load(fullUrl)
            .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                @Override
                public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                    try {
                        java.io.File cachePath = new java.io.File(getCacheDir(), "images");
                        cachePath.mkdirs();
                        java.io.File imageFile = new java.io.File(cachePath, "share_image_detail.png");
                        java.io.FileOutputStream stream = new java.io.FileOutputStream(imageFile);
                        resource.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();
                        triggerShare(imageFile, shareText);
                    } catch (java.io.IOException e) {
                        shareAsText(shareText);
                    }
                }

                @Override
                public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}

                @Override
                public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                    shareAsText(shareText);
                }
            });
    }

    private void triggerShare(java.io.File imageFile, String text) {
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

    private void setupTimeline() {
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupPhotoList() {
        photoAdapter = new PhotoAdapter(null); // No removal in detail
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
                
                // Hide shimmer once main data is loaded
                binding.layoutShimmer.getRoot().setVisibility(View.GONE);
                binding.scrollContent.setVisibility(View.VISIBLE);
            }
        });

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
    }

    private void updateTimeline(Complaint complaint) {
        List<TimelineAdapter.StatusPoint> points = new ArrayList<>();
        long created = complaint.getCreatedAt() > 0 ? complaint.getCreatedAt() : System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(created));
        String pending = getString(R.string.status_pending);

        String currentStatus = complaint.getStatus() != null ? complaint.getStatus() : "SUBMITTED";

        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_submitted), dateStr, true));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_reviewed), pending, isAtLeast(currentStatus, "REVIEWED")));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_assigned), pending, isAtLeast(currentStatus, "ASSIGNED"), complaint.getAssignedTo()));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_in_progress), pending, isAtLeast(currentStatus, "IN_PROGRESS")));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_resolved), pending, isAtLeast(currentStatus, "RESOLVED")));

        binding.rvTimeline.setAdapter(new TimelineAdapter(points));
    }

    private boolean isAtLeast(String currentStatus, String targetStatus) {
        List<String> order = List.of("SUBMITTED", "REVIEWED", "ASSIGNED", "IN_PROGRESS", "RESOLVED");
        int currentIndex = order.indexOf(currentStatus);
        int targetIndex = order.indexOf(targetStatus);
        return currentIndex >= targetIndex;
    }

    private void setupListeners() {
        binding.btnSupport.setOnClickListener(v -> {
            com.voiceyanga.citizen.core.utils.HapticHelper.performSuccess(v);
            viewModel.supportComplaint(complaintUuid);
            viewModel.simulateProgress(complaintUuid); // Mock update for UI verification
            
            // Change button state to "Supported" (Red & Disabled)
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
                viewModel.postComment(complaintUuid, message, true);
            }
        });
    }
}