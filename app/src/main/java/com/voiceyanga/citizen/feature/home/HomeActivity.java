package com.voiceyanga.citizen.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.databinding.ActivityHomeBinding;
import com.voiceyanga.citizen.feature.auth.LoginActivity;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.feature.complaints.ComplaintDetailActivity;
import com.voiceyanga.citizen.feature.complaints.CreateComplaintActivity;
import com.voiceyanga.citizen.feature.notifications.NotificationCenterActivity;
import com.voiceyanga.citizen.feature.profile.ProfileActivity;
import java.util.Locale;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private ComplaintAdapter adapter;

    @Inject
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        // Ensure icons show original colors
        binding.navView.setItemIconTintList(null);
        
        setupHeader();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        handleDeepLink();
    }

    private void handleDeepLink() {
        if (getIntent().hasExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID)) {
            String uuid = getIntent().getStringExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID);
            Intent intent = new Intent(this, ComplaintDetailActivity.class);
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, uuid);
            startActivity(intent);
        }
    }

    private void setupHeader() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        binding.tvGreeting.setText(String.format(getString(R.string.greeting_format), name));
        
        // Update Nav Drawer Header
        View headerView = binding.navView.getHeaderView(0);
        TextView tvNavName = headerView.findViewById(R.id.tvNavUserName);
        TextView tvNavEmail = headerView.findViewById(R.id.tvNavUserEmail);
        if (tvNavName != null) tvNavName.setText(name);
        if (tvNavEmail != null) tvNavEmail.setText(email);

        // Mock critical badge
        binding.tvCriticalBadge.setText(String.format(Locale.getDefault(), getString(R.string.critical_issues_badge), 3));
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(complaint -> {
            Intent intent = new Intent(this, ComplaintDetailActivity.class);
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
            startActivity(intent);
        });
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getComplaints().observe(this, complaints -> {
            adapter.submitList(complaints);
            binding.llEmptyState.setVisibility(
                    (complaints == null || complaints.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupListeners() {
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_my_complaints) {
                startActivity(new Intent(this, MyComplaintsActivity.class));
            } else if (id == R.id.nav_help || id == R.id.nav_about || id == R.id.nav_privacy) {
                String message = String.format(getString(R.string.coming_soon_format), item.getTitle());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.retrySync());
        
        View.OnClickListener reportListener = v -> {
            Intent intent = new Intent(this, CreateComplaintActivity.class);
            startActivity(intent);
        };

        binding.fabReport.setOnClickListener(reportListener);
        binding.btnReportNow.setOnClickListener(reportListener);
        binding.cardReport.setOnClickListener(reportListener);

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationCenterActivity.class)));
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}