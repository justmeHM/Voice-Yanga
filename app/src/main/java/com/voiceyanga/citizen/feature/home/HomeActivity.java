package com.voiceyanga.citizen.feature.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.voiceyanga.citizen.databinding.ActivityHomeBinding;
import com.voiceyanga.citizen.feature.complaints.CreateComplaintActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private ComplaintAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter();
        binding.rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComplaints.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getComplaints().observe(this, complaints -> {
            adapter.submitList(complaints);
            binding.tvPlaceholder.setVisibility(
                    (complaints == null || complaints.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    private void setupListeners() {
        binding.fabReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateComplaintActivity.class);
            startActivity(intent);
        });
    }
}