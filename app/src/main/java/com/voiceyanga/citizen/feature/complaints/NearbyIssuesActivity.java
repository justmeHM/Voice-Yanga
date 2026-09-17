package com.voiceyanga.citizen.feature.complaints;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.gms.location.LocationServices;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ActivityNearbyIssuesBinding;
import com.voiceyanga.citizen.feature.home.ComplaintAdapter;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NearbyIssuesActivity extends AppCompatActivity {

    private ActivityNearbyIssuesBinding binding;
    private NearbyIssuesViewModel viewModel;
    private ComplaintAdapter adapter;

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

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
        fetchUserLocation();
        viewModel.refreshData();
    }

    private void fetchUserLocation() {
        binding.swipeRefresh.setRefreshing(true);
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(location -> {
                binding.swipeRefresh.setRefreshing(false);
                if (location != null) {
                    viewModel.setUserLocation(location);
                } else {
                    // Fallback to Lusaka center
                    android.location.Location fallback = new android.location.Location("");
                    fallback.setLatitude(-15.4167);
                    fallback.setLongitude(28.2833);
                    viewModel.setUserLocation(fallback);
                }
            }).addOnFailureListener(e -> {
                binding.swipeRefresh.setRefreshing(false);
            });
        } else {
            binding.swipeRefresh.setRefreshing(false);
            // Fallback
            android.location.Location fallback = new android.location.Location("");
            fallback.setLatitude(-15.4167);
            fallback.setLongitude(28.2833);
            viewModel.setUserLocation(fallback);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(new ComplaintAdapter.OnComplaintClickListener() {
            @Override
            public void onComplaintClick(Complaint complaint, View sharedElement) {
                Intent intent = new Intent(NearbyIssuesActivity.this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                
                if (sharedElement != null && sharedElement.getVisibility() == View.VISIBLE) {
                    androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            NearbyIssuesActivity.this, sharedElement, sharedElement.getTransitionName());
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }

            @Override
            public void onSupportClick(Complaint complaint) {
                viewModel.supportComplaint(complaint.getClientUuid());
                Toast.makeText(NearbyIssuesActivity.this, R.string.support_thanks, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRetryClick(Complaint complaint) {
                viewModel.retryComplaint(complaint.getClientUuid());
                Toast.makeText(NearbyIssuesActivity.this, "Retrying sync...", Toast.LENGTH_SHORT).show();
            }
        }, sessionManager.getUserEmail());
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
        binding.swipeRefresh.setOnRefreshListener(() -> {
            fetchUserLocation();
            viewModel.refreshData();
            viewModel.retrySync();
        });
    }
}
