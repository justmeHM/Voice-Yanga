package com.voiceyanga.citizen.feature.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.voiceyanga.citizen.databinding.ActivityNotificationCenterBinding;
import com.voiceyanga.citizen.feature.complaints.ComplaintDetailActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationCenterActivity extends AppCompatActivity {

    @javax.inject.Inject
    com.voiceyanga.citizen.data.repository.AnalyticsRepository analyticsRepository;

    private ActivityNotificationCenterBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        setupToolbar();
        setupTabs();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String type = "ALL";
                switch (tab.getPosition()) {
                    case 0: type = "ALL"; break;
                    case 1: type = "STATUS_UPDATES"; break;
                    case 2: type = "COMMENTS_AND_SUPPORT"; break;
                }
                viewModel.setFilterType(type);
                
                // Safe diagnostic for UI
                android.util.Log.d("NotificationUI", String.format("NOTIFICATIONS_UI activeFilter=%s", type));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvMarkAllRead.setOnClickListener(v -> viewModel.markAllAsRead());
        binding.layoutError.btnRetry.setOnClickListener(v -> viewModel.refresh());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notification -> {
            viewModel.markAsRead(notification.getId());
            if (analyticsRepository != null) {
                analyticsRepository.trackNotificationOpened("IN_APP", notification.getType(), "OPEN_DETAIL");
            }
            if (notification.getComplaintUuid() != null) {
                Intent intent = new Intent(this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, notification.getComplaintUuid());
                intent.putExtra("view_context", "NOTIFICATION");
                startActivity(intent);
            }
        });
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                com.voiceyanga.citizen.data.local.entity.Notification notification = adapter.getNotificationAt(position);
                if (notification != null) {
                    viewModel.markAsRead(notification.getId());
                }
            }
        }).attachToRecyclerView(binding.rvNotifications);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            if (loading != null && loading) {
                binding.llEmptyState.setVisibility(View.GONE);
                // Maybe show a progress bar if one exists, but keeping minimal changes
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                binding.layoutError.llErrorRoot.setVisibility(View.VISIBLE);
                binding.layoutError.tvErrorMessage.setText(error);
                binding.rvNotifications.setVisibility(View.GONE);
                binding.llEmptyState.setVisibility(View.GONE);
            } else {
                binding.layoutError.llErrorRoot.setVisibility(View.GONE);
                binding.rvNotifications.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getNotifications().observe(this, notifications -> {
            adapter.setNotifications(notifications);
            
            Boolean loading = viewModel.getLoading().getValue();
            boolean isLoading = loading != null && loading;
            
            if (!isLoading) {
                binding.llEmptyState.setVisibility(
                        (notifications == null || notifications.isEmpty()) ? View.VISIBLE : View.GONE);
            }
            
            binding.tvMarkAllRead.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.GONE : View.VISIBLE);
            
            // Safe diagnostic for UI
            android.util.Log.d("NotificationUI", String.format("NOTIFICATIONS_UI renderedCount=%d",
                    notifications != null ? notifications.size() : 0));
        });
    }
}