package com.voiceyanga.citizen.feature.home;

import android.content.Intent;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.entity.Complaint;
import com.voiceyanga.citizen.databinding.ActivityHomeBinding;
import com.voiceyanga.citizen.feature.auth.LoginActivity;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.feature.complaints.ComplaintDetailActivity;
import com.voiceyanga.citizen.feature.complaints.CreateComplaintActivity;
import com.voiceyanga.citizen.feature.complaints.NearbyIssuesActivity;
import com.voiceyanga.citizen.feature.notifications.NotificationCenterActivity;
import com.voiceyanga.citizen.feature.profile.ProfileActivity;
import com.voiceyanga.citizen.ui.common.AboutActivity;
import com.voiceyanga.citizen.ui.common.SupportActivity;
import com.voiceyanga.citizen.feature.complaints.OutboxActivity;
import java.util.Calendar;
import java.util.Locale;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private ComplaintAdapter adapter;
    private boolean isFabExpanded = false;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Inject
    SessionManager sessionManager;

    @Inject
    AuthRepository authRepository;

    private android.view.GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayoutParent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        // Ensure icons show original colors
        binding.navView.setItemIconTintList(null);
        
        setupHeader();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        setupGestures();
        setupScrollListener();
        handleDeepLink();
        setupNetworkListener();
        setupSwipeRefresh();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.primary_green),
                ContextCompat.getColor(this, R.color.primary_red),
                ContextCompat.getColor(this, R.color.status_pending_text)
        );
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this, R.color.primary_green_light)
        );
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
                    if (diffX > 0) { // Right Swipe
                        startActivity(new Intent(HomeActivity.this, MyComplaintsActivity.class));
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

    private void setupNetworkListener() {
        binding.tvOfflineBanner.setOnClickListener(v -> {
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), 
                    "Reports are queued and will upload automatically when you're back online.", 
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
        });
        
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> binding.tvOfflineBanner.setVisibility(View.GONE));
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> binding.tvOfflineBanner.setVisibility(View.VISIBLE));
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
        
        // Initial check
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        boolean online = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        binding.tvOfflineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
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
        
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good morning";
        else if (hour < 17) greeting = "Good afternoon";
        else greeting = "Good evening";

        binding.tvGreeting.setText(String.format("%s, %s", greeting, name));
        
        // Update Nav Drawer Header
        View headerView = binding.navView.getHeaderView(0);
        TextView tvNavName = headerView.findViewById(R.id.tvNavUserName);
        TextView tvNavEmail = headerView.findViewById(R.id.tvNavUserEmail);
        if (tvNavName != null) tvNavName.setText(name);
        if (tvNavEmail != null) tvNavEmail.setText(email);
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(new ComplaintAdapter.OnComplaintClickListener() {
            @Override
            public void onComplaintClick(Complaint complaint, android.view.View sharedElement) {
                Intent intent = new Intent(HomeActivity.this, ComplaintDetailActivity.class);
                intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                startActivity(intent);
            }

            @Override
            public void onSupportClick(Complaint complaint) {
                viewModel.supportComplaint(complaint.getClientUuid());
                Toast.makeText(HomeActivity.this, R.string.support_thanks, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShareClick(Complaint complaint) {
                String photoUri = complaint.getFirstPhotoUri();
                String shareText = String.format("Help me get this issue noticed! %s in %s. Reported via Voice Yanga app.", 
                        complaint.getTitle(), complaint.getLocation());

                if (photoUri != null && !photoUri.isEmpty()) {
                    if (photoUri.startsWith("http")) {
                        // For server images, we must load via Glide
                        shareWithGlide(photoUri, shareText);
                    } else {
                        // For local images, try to access directly or copy to shareable location
                        try {
                            java.io.File cachePath = new java.io.File(getCacheDir(), "images");
                            cachePath.mkdirs();
                            java.io.File shareFile = new java.io.File(cachePath, "share_image.png");
                            
                            java.io.InputStream is;
                            if (photoUri.startsWith("content://")) {
                                is = getContentResolver().openInputStream(android.net.Uri.parse(photoUri));
                            } else {
                                is = new java.io.FileInputStream(photoUri);
                            }
                            
                            if (is != null) {
                                java.io.FileOutputStream os = new java.io.FileOutputStream(shareFile);
                                byte[] buffer = new byte[8192];
                                int read;
                                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
                                is.close();
                                os.close();
                                
                                triggerShare(shareFile, shareText);
                            } else {
                                shareWithGlide(photoUri, shareText);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("HomeActivity", "Direct share failed, falling back to Glide", e);
                            shareWithGlide(photoUri, shareText);
                        }
                    }
                } else {
                    shareAsText(shareText);
                }
            }

            private void shareWithGlide(String url, String shareText) {
                String fullUrl = url;
                if (!url.startsWith("http") && !url.startsWith("content://") && !url.startsWith("file://") && !url.startsWith("/")) {
                    fullUrl = com.voiceyanga.citizen.core.network.ApiConstants.API_HOST + "/" + url;
                }

                com.bumptech.glide.Glide.with(HomeActivity.this)
                    .asBitmap()
                    .load(fullUrl)
                    .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                        @Override
                        public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                            try {
                                java.io.File cachePath = new java.io.File(getCacheDir(), "images");
                                cachePath.mkdirs();
                                java.io.File imageFile = new java.io.File(cachePath, "share_image.png");
                                java.io.FileOutputStream stream = new java.io.FileOutputStream(imageFile);
                                resource.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                                stream.close();
                                triggerShare(imageFile, shareText);
                            } catch (java.io.IOException e) {
                                shareAsText(shareText);
                            }
                        }

                        @Override
                        public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}

                        @Override
                        public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                            shareAsText(shareText);
                        }
                    });
            }

            private void triggerShare(java.io.File imageFile, String text) {
                runOnUiThread(() -> {
                    android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(HomeActivity.this, getPackageName() + ".provider", imageFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(shareIntent, "Share Report"));
                });
            }

            private void shareAsText(String text) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(intent, "Share via"));
            }

            @Override
            public void onRetryClick(Complaint complaint) {
                viewModel.retryComplaint(complaint.getClientUuid());
                Toast.makeText(HomeActivity.this, "Retrying sync...", Toast.LENGTH_SHORT).show();
            }
        }, sessionManager.getUserEmail());
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                binding.layoutError.llErrorRoot.setVisibility(View.VISIBLE);
                binding.layoutError.tvErrorMessage.setText(error);
                binding.rvComplaints.setVisibility(View.GONE);
                binding.shimmerFeed.setVisibility(View.GONE);
            } else {
                binding.layoutError.llErrorRoot.setVisibility(View.GONE);
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    binding.shimmerFeed.startShimmer();
                    binding.shimmerFeed.setVisibility(View.VISIBLE);
                    binding.rvComplaints.setVisibility(View.GONE);
                } else {
                    binding.shimmerFeed.stopShimmer();
                    binding.shimmerFeed.setVisibility(View.GONE);
                    binding.rvComplaints.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getComplaints().observe(this, complaints -> {
            // When data arrives, stop loading
            viewModel.setLoading(false);
            
            adapter.submitList(complaints);
            binding.llEmptyState.setVisibility(
                    (complaints == null || complaints.isEmpty()) ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });

        viewModel.getLatestMyComplaint().observe(this, complaint -> {
            if (complaint != null) {
                binding.cardLatestReport.setVisibility(View.VISIBLE);
                binding.tvLatestTitle.setText(complaint.getTitle());
                binding.tvLatestStatus.setText(complaint.getStatus());
                
                // Format relative time
                long diff = System.currentTimeMillis() - complaint.getCreatedAt();
                String timeStr;
                if (diff < 60000) timeStr = "Just now";
                else if (diff < 3600000) timeStr = (diff / 60000) + "m ago";
                else if (diff < 86400000) timeStr = (diff / 3600000) + "h ago";
                else timeStr = (diff / 86400000) + "d ago";
                
                binding.tvLatestDate.setText(timeStr);
                
                binding.cardLatestReport.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ComplaintDetailActivity.class);
                    intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
                    startActivity(intent);
                });
            } else {
                binding.cardLatestReport.setVisibility(View.GONE);
            }
        });

        viewModel.getOutboxComplaints().observe(this, outbox -> {
            if (outbox != null && !outbox.isEmpty()) {
                binding.cardOutbox.setVisibility(View.VISIBLE);
                binding.tvOutboxCount.setText(String.format(Locale.getDefault(), 
                        "You have %d report%s waiting to sync", 
                        outbox.size(), outbox.size() == 1 ? "" : "s"));
                binding.btnViewOutbox.setOnClickListener(v -> {
                    startActivity(new Intent(this, OutboxActivity.class));
                });
            } else {
                binding.cardOutbox.setVisibility(View.GONE);
            }
        });
    }

    private void setupListeners() {
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.fabMenuScrim.setOnClickListener(v -> toggleFabMenu());

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_my_complaints) {
                startActivity(new Intent(this, MyComplaintsActivity.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, SupportActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (id == R.id.nav_privacy) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://voiceyanga.netlify.app/"));
                startActivity(intent);
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.retrySync());
        
        binding.fabReport.setOnClickListener(v -> toggleFabMenu());

        binding.fabReportProblem.setOnClickListener(v -> {
            toggleFabMenu();
            startActivity(new Intent(this, CreateComplaintActivity.class));
        });

        binding.btnExploreNearby.setOnClickListener(v -> 
            startActivity(new Intent(this, NearbyIssuesActivity.class)));

        binding.fabNearbyIssues.setOnClickListener(v -> {
            toggleFabMenu();
            startActivity(new Intent(this, NearbyIssuesActivity.class));
        });

        binding.btnReportNow.setOnClickListener(v -> 
            startActivity(new Intent(this, CreateComplaintActivity.class)));
        
        binding.cardReport.setOnClickListener(v -> 
            startActivity(new Intent(this, CreateComplaintActivity.class)));

        binding.btnNotifications.setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationCenterActivity.class)));

        binding.btnFilter.setOnClickListener(v -> {
            FilterBottomSheet filterSheet = new FilterBottomSheet();
            filterSheet.show(getSupportFragmentManager(), "filter_sheet");
        });

        binding.btnSeeMore.setOnClickListener(v -> 
            startActivity(new Intent(this, MyComplaintsActivity.class)));

        binding.layoutError.btnRetry.setOnClickListener(v -> viewModel.retrySync());
    }

    private void setupScrollListener() {
        binding.rvComplaints.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                // Keep hide/show logic primarily in onScrollStateChanged for maximum stability
            }

            @Override
            public void onScrollStateChanged(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isFabExpanded) return;

                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING || 
                    newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING) {
                    binding.fabReport.hide();
                } else if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                    binding.fabReport.show();
                }
            }
        });
    }

    private void toggleFabMenu() {
        isFabExpanded = !isFabExpanded;
        
        com.voiceyanga.citizen.core.utils.HapticHelper.selection(binding.fabReport);

        if (isFabExpanded) {
            // Background Dimming
            binding.fabMenuScrim.setVisibility(View.VISIBLE);
            binding.fabMenuScrim.setAlpha(0f);
            binding.fabMenuScrim.animate().alpha(1f).setDuration(200).start();

            // Rotate main FAB icon
            binding.fabReport.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            
            // Cascading Animation for Rows
            binding.llReportProblemRow.setVisibility(View.VISIBLE);
            binding.llReportProblemRow.setAlpha(0f);
            binding.llReportProblemRow.setTranslationY(20f);
            binding.llReportProblemRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .start();

            binding.llNearbyIssuesRow.setVisibility(View.VISIBLE);
            binding.llNearbyIssuesRow.setAlpha(0f);
            binding.llNearbyIssuesRow.setTranslationY(20f);
            binding.llNearbyIssuesRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setStartDelay(100)
                    .start();
            
        } else {
            // Background Brightening
            binding.fabMenuScrim.animate().alpha(0f).setDuration(200).withEndAction(() -> 
                binding.fabMenuScrim.setVisibility(View.GONE)).start();

            // Revert main FAB icon
            binding.fabReport.setImageResource(android.R.drawable.ic_input_add);
            
            // Hide Rows
            binding.llReportProblemRow.setVisibility(View.GONE);
            binding.llNearbyIssuesRow.setVisibility(View.GONE);
        }
    }

    private void logout() {
        authRepository.logout(success -> {
            runOnUiThread(() -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
