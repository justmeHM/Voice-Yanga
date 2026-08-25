package com.voiceyanga.citizen.feature.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        viewModel.getNotifications().observe(this, notifications -> {
            adapter.setNotifications(notifications);
            binding.llEmptyState.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.tvMarkAllRead.setVisibility(
                    (notifications == null || notifications.isEmpty()) ? View.GONE : View.VISIBLE);
        });
    }
}