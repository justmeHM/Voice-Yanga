package com.voiceyanga.citizen.feature.complaints;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.databinding.ActivitySuccessGalleryBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SuccessGalleryActivity extends AppCompatActivity {

    private ActivitySuccessGalleryBinding binding;
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuccessGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new GalleryAdapter(complaint -> {
            android.content.Intent intent = new android.content.Intent(this, ComplaintDetailActivity.class);
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_UUID, complaint.getClientUuid());
            startActivity(intent);
        });
        binding.rvGallery.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGallery.setAdapter(adapter);
    }

    private void observeViewModel() {
        SuccessGalleryViewModel viewModel = new ViewModelProvider(this).get(SuccessGalleryViewModel.class);
        viewModel.getSuccessStories().observe(this, stories -> {
            adapter.submitList(stories);
            binding.llEmptyState.setVisibility(
                    (stories == null || stories.isEmpty()) ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.rvGallery.setVisibility(
                    (stories == null || stories.isEmpty()) ? android.view.View.GONE : android.view.View.VISIBLE);
        });
    }
}
