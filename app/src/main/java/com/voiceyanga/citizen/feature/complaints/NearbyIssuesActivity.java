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
import com.voiceyanga.citizen.databinding.ActivityNearbyIssuesBinding;
import com.voiceyanga.citizen.feature.home.ComplaintAdapter;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NearbyIssuesActivity extends AppCompatActivity {

    private ActivityNearbyIssuesBinding binding;
    private NearbyIssuesViewModel viewModel;
    private ComplaintAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNearbyIssuesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NearbyIssuesViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(new ComplaintAdapter.OnComplaintClickListener() {
            @Override
            public void onComplaintClick(Complaint complaint) {
                Intent intent = new Intent(NearbyIssuesActivity.this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                startActivity(intent);
            }

            @Override
            public void onSupportClick(Complaint complaint) {
                viewModel.supportComplaint(complaint.getClientUuid());
                Toast.makeText(NearbyIssuesActivity.this, R.string.support_thanks, Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getNearbyComplaints().observe(this, complaints -> {
            adapter.submitList(complaints);
            binding.llEmptyState.setVisibility(
                    (complaints == null || complaints.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.retrySync());
    }
}
