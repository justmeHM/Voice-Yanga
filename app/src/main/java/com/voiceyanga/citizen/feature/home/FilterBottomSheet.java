package com.voiceyanga.citizen.feature.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import com.voiceyanga.citizen.databinding.LayoutFilterBottomSheetBinding;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FilterBottomSheet extends BottomSheetDialogFragment {

    private LayoutFilterBottomSheetBinding binding;
    private HomeViewModel viewModel;
    private List<LocationDto> allLocations = new ArrayList<>();

    @Inject
    ReferenceRepository referenceRepository;

    public FilterBottomSheet() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LayoutFilterBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        setupInitialState();
        loadLocations();

        binding.btnClear.setOnClickListener(v -> {
            viewModel.clearFilters();
            dismiss();
        });

        binding.btnApply.setOnClickListener(v -> {
            String province = binding.actProvince.getText().toString().trim();
            String district = binding.actDistrict.getText().toString().trim();
            String ward = binding.actWard.getText().toString().trim();

            viewModel.setFilter("province", province.isEmpty() ? null : province);
            viewModel.setFilter("district", district.isEmpty() ? null : district);
            viewModel.setFilter("ward", ward.isEmpty() ? null : ward);
            
            android.util.Log.d("FilterSheet", String.format("Applying: %s, %s, %s", province, district, ward));
            dismiss();
        });
    }

    private void setupInitialState() {
        Map<String, String> currentFilters = viewModel.getFilters().getValue();
        if (currentFilters != null) {
            binding.actProvince.setText(currentFilters.getOrDefault("province", ""), false);
            binding.actDistrict.setText(currentFilters.getOrDefault("district", ""), false);
            binding.actWard.setText(currentFilters.getOrDefault("ward", ""), false);
        }
    }

    private void loadLocations() {
        referenceRepository.getLocations(new ReferenceRepository.ReferenceCallback<List<LocationDto>>() {
            @Override
            public void onSuccess(List<LocationDto> data) {
                allLocations = data;
                updateDropdowns();
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Error loading locations: " + message, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateDropdowns() {
        if (getContext() == null) return;

        // Province Dropdown
        Set<String> provinces = allLocations.stream()
                .map(LocationDto::getProvince)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        
        binding.actProvince.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(provinces)));

        // District Dropdown
        Set<String> districts = allLocations.stream()
                .map(LocationDto::getDistrict)
                .filter(d -> d != null && !d.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        binding.actDistrict.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(districts)));

        // Ward Dropdown
        Set<String> wards = allLocations.stream()
                .map(LocationDto::getWard)
                .filter(w -> w != null && !w.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        binding.actWard.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(wards)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
