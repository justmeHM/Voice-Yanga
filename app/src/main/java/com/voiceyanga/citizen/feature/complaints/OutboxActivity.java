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
                showOutboxActionDialog(complaint);
            }

            @Override
            public void onRetryClick(Complaint complaint) {
                showOutboxActionDialog(complaint);
            }
        }, viewModel.getRepository());
        binding.rvOutbox.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOutbox.setAdapter(adapter);
    }

    private void showOutboxActionDialog(Complaint complaint) {
        String[] options = {"Retry Upload", "Edit Report", "Cancel / Discard Submission"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(complaint.getTitle() != null && !complaint.getTitle().isEmpty() ? complaint.getTitle() : "Pending Submission")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.retryComplaint(complaint.getClientUuid());
                        Toast.makeText(this, "Retrying upload...", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        Intent intent = new Intent(OutboxActivity.this, CreateComplaintActivity.class);
                        intent.putExtra("extra_complaint_uuid", complaint.getClientUuid());
                        startActivity(intent);
                        finish();
                    } else {
                        confirmDiscardSubmission(complaint);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDiscardSubmission(Complaint complaint) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Discard Pending Report?")
                .setMessage("Are you sure you want to cancel and delete this pending submission? It will be removed permanently.")
                .setPositiveButton("Discard Report", (dialog, which) -> {
                    viewModel.deleteComplaint(complaint.getClientUuid());
                    Toast.makeText(this, "Pending submission discarded", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
