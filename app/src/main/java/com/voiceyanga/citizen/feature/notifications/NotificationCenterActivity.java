package com.voiceyanga.citizen.feature.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
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
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        setupToolbar();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvMarkAllRead.setOnClickListener(v -> viewModel.markAllAsRead());
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
    }

    private void observeViewModel() {
        viewModel.getNotifications().observe(this, notifications -> {
            adapter.setNotifications(notifications);
            binding.llEmptyState.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.tvMarkAllRead.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.GONE : View.VISIBLE);
        });
    }
}