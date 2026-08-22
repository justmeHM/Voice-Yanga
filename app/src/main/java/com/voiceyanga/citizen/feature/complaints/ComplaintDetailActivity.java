package com.voiceyanga.citizen.feature.complaints;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ActivityComplaintDetailBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ComplaintDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMPLAINT_UUID = "extra_complaint_uuid";

    private ActivityComplaintDetailBinding binding;
    private ComplaintDetailViewModel viewModel;
    private String complaintUuid;

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
        observeViewModel();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTimeline() {
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
    }

    private void observeViewModel() {
        viewModel.getComplaint(complaintUuid).observe(this, complaint -> {
            if (complaint != null) {
                displayComplaint(complaint);
            }
        });
    }

    private void displayComplaint(Complaint complaint) {
        binding.tvRefCode.setText(complaint.getReferenceCode() != null ? complaint.getReferenceCode() : "PENDING SYNC");
        binding.tvTitle.setText(complaint.getTitle());
        binding.tvCategory.setText(complaint.getCategory().toUpperCase());
        binding.tvLocation.setText(complaint.getLocation());
        binding.tvDescription.setText(complaint.getDescription());
        binding.tvPriority.setText(String.format(getString(R.string.priority_format), complaint.getPriority()));
        
        binding.btnSupport.setText(String.format(Locale.getDefault(), getString(R.string.support_count_format), complaint.getSupportCount()));

        updateTimeline(complaint);
    }

    private void updateTimeline(Complaint complaint) {
        List<TimelineAdapter.StatusPoint> points = new ArrayList<>();
        String dateStr = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date(complaint.getCreatedAt()));

        // In a real app, these dates would come from the server history
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_submitted), dateStr, true));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_reviewed), "Pending", isAtLeast(complaint.getStatus(), "REVIEWED")));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_assigned), "Pending", isAtLeast(complaint.getStatus(), "ASSIGNED")));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_in_progress), "Pending", isAtLeast(complaint.getStatus(), "IN_PROGRESS")));
        points.add(new TimelineAdapter.StatusPoint(getString(R.string.status_resolved), "Pending", isAtLeast(complaint.getStatus(), "RESOLVED")));

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
            Toast.makeText(this, "Thank you for your support!", Toast.LENGTH_SHORT).show();
        });
    }
}