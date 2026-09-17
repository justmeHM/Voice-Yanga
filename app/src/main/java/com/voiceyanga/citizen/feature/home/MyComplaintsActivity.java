package com.voiceyanga.citizen.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.voiceyanga.citizen.R;
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

    private android.view.GestureDetector gestureDetector;

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.refreshData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMyComplaintsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(MyComplaintsViewModel.class);

        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();
        setupGestures();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshData();
        });
    }

    private void setupGestures() {
        gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                
                // HIGHER THRESHOLDS for deliberate swiping (300px, 2000 velocity)
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 300 && Math.abs(velocityX) > 2000) {
                    if (diffX < 0) { // Left Swipe (Right to Left)
                        finish();
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        viewModel.setSelectedTab(MyComplaintsViewModel.Tab.ACTIVE);
                        break;
                    case 1:
                        viewModel.setSelectedTab(MyComplaintsViewModel.Tab.RESOLVED);
                        break;
                    case 2:
                        viewModel.setSelectedTab(MyComplaintsViewModel.Tab.SUPPORTED);
                        break;
                }
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
            public void onComplaintClick(Complaint complaint, View sharedElement) {
                Intent intent = new Intent(MyComplaintsActivity.this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                
                if (sharedElement != null && sharedElement.getVisibility() == View.VISIBLE) {
                    androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            MyComplaintsActivity.this, sharedElement, sharedElement.getTransitionName());
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }

            @Override
            public void onRetryClick(Complaint complaint) {
                viewModel.retrySync(complaint.getClientUuid());
                android.widget.Toast.makeText(MyComplaintsActivity.this, "Retrying sync...", android.widget.Toast.LENGTH_SHORT).show();
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
            viewModel.setLoading(false);
            binding.swipeRefresh.setRefreshing(false);
            
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