package com.voiceyanga.citizen.feature.complaints;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ActivityOutboxBinding;
import com.voiceyanga.citizen.feature.home.ComplaintAdapter;
import com.voiceyanga.citizen.feature.home.HomeViewModel;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OutboxActivity extends AppCompatActivity {

    private ActivityOutboxBinding binding;
    private HomeViewModel viewModel;
    private ComplaintAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOutboxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(new ComplaintAdapter.OnComplaintClickListener() {
            @Override
            public void onComplaintClick(Complaint complaint, View sharedElement) {
                if ("DRAFT".equals(complaint.getSyncStatus())) {
                    startActivity(new Intent(OutboxActivity.this, CreateComplaintActivity.class));
                    finish();
                } else {
                    Intent intent = new Intent(OutboxActivity.this, ComplaintDetailActivity.class);
                    intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                    startActivity(intent);
                }
            }

            @Override
            public void onRetryClick(Complaint complaint) {
                viewModel.retryComplaint(complaint.getClientUuid());
                Toast.makeText(OutboxActivity.this, "Retrying sync...", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvOutbox.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOutbox.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getOutboxComplaints().observe(this, complaints -> {
            adapter.submitList(complaints);
            if (complaints == null || complaints.isEmpty()) {
                finish(); // Close outbox if empty
            }
        });
    }
}
