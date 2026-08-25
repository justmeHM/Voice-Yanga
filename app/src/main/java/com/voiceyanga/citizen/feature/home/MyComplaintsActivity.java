package com.voiceyanga.citizen.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ActivityMyComplaintsBinding;
import com.voiceyanga.citizen.feature.complaints.ComplaintDetailActivity;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyComplaintsActivity extends AppCompatActivity {

    private ActivityMyComplaintsBinding binding;
    private MyComplaintsViewModel viewModel;
    private ComplaintAdapter adapter;

    @Inject
    com.voiceyanga.citizen.data.local.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyComplaintsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MyComplaintsViewModel.class);

        setupToolbar();
        setupTabs();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setShowResolved(tab.getPosition() == 1);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(new ComplaintAdapter.OnComplaintClickListener() {
            @Override
            public void onComplaintClick(Complaint complaint) {
                Intent intent = new Intent(MyComplaintsActivity.this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                startActivity(intent);
            }
        }, sessionManager.getUserEmail());
        binding.rvMyComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyComplaints.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    binding.shimmerMyComplaints.startShimmer();
                    binding.shimmerMyComplaints.setVisibility(View.VISIBLE);
                    binding.rvMyComplaints.setVisibility(View.GONE);
                } else {
                    binding.shimmerMyComplaints.stopShimmer();
                    binding.shimmerMyComplaints.setVisibility(View.GONE);
                    binding.rvMyComplaints.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getMyComplaints().observe(this, complaints -> {
            // When data arrives, stop loading
            ((androidx.lifecycle.MutableLiveData<Boolean>)viewModel.getLoading()).setValue(false);
            
            adapter.submitList(complaints);
            binding.llEmptyState.setVisibility(
                    (complaints == null || complaints.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getMyReportsCount().observe(this, count -> {
            binding.tvStatMyReports.setText(String.valueOf(count));
        });

        viewModel.getSupportedCount().observe(this, count -> {
            binding.tvStatSupported.setText(String.valueOf(count));
        });
    }
}