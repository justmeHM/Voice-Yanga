package com.voiceyanga.citizen.feature.complaints;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
import android.graphics.Color;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                displayComplaint(complaint);
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
    }

    private void displayComplaint(Complaint complaint) {
        binding.tvRefCode.setText(complaint.getReferenceCode() != null ? complaint.getReferenceCode() : "PENDING SYNC");
        binding.tvTitle.setText(complaint.getTitle());
        
        String cat = complaint.getCategory();
        binding.tvCategory.setText(cat != null ? cat.toUpperCase() : "GENERAL");
        
        binding.tvLocation.setText(complaint.getLocation() != null ? complaint.getLocation() : "Unknown");
        binding.tvDescription.setText(complaint.getDescription());
        binding.tvPriority.setText(String.format(getString(R.string.priority_format), 
                complaint.getPriority() != null ? complaint.getPriority() : "MEDIUM"));
        
        binding.btnSupport.setText(String.format(Locale.getDefault(), getString(R.string.support_count_format), complaint.getSupportCount()));

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
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_assigned), pending, isAtLeast(currentStatus, "ASSIGNED")));
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
            viewModel.supportComplaint(complaintUuid);
            viewModel.simulateProgress(complaintUuid); // Mock update for UI verification
            
            // Change button state to "Supported" (Red & Disabled)
            binding.btnSupport.setEnabled(false);
            binding.btnSupport.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_red)));
            binding.btnSupport.setTextColor(Color.WHITE);
            binding.btnSupport.setIconTintResource(android.R.color.white);
            
            Toast.makeText(this, R.string.support_thanks, Toast.LENGTH_SHORT).show();
        });
    }
}