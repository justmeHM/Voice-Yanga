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
                switch (tab.getPosition()) {
                    case 0: viewModel.setFilterType("ALL"); break;
                    case 1: viewModel.setFilterType("STATUS_CHANGE"); break;
                    case 2: viewModel.setFilterType("NEW_COMMENT"); break;
                }
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
        binding.layoutError.btnRetry.setOnClickListener(v -> viewModel.getNotifications());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notification -> {
            viewModel.markAsRead(notification.getId());
            if (notification.getComplaintUuid() != null) {
                Intent intent = new Intent(this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, notification.getComplaintUuid());
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
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                binding.layoutError.llErrorRoot.setVisibility(View.VISIBLE);
                binding.layoutError.tvErrorMessage.setText(error);
                binding.rvNotifications.setVisibility(View.GONE);
            } else {
                binding.layoutError.llErrorRoot.setVisibility(View.GONE);
            }
        });

        viewModel.getNotifications().observe(this, notifications -> {
            adapter.setNotifications(notifications);
            binding.llEmptyState.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.tvMarkAllRead.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.GONE : View.VISIBLE);
        });
    }
}