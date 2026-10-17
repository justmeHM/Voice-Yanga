package com.voiceyanga.citizen.feature.complaints;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.remote.dto.CategoryDto;
import com.voiceyanga.citizen.databinding.ItemCategoryBinding;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategorySelectedListener {
        void onCategorySelected(CategoryDto category);
    }

    private List<CategoryDto> categories = new ArrayList<>();
    private final OnCategorySelectedListener listener;
    private int selectedPosition = -1;
    private String selectedCategoryName = null;

    public CategoryAdapter(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<CategoryDto> categories) {
        if (categories == null) return;
        List<CategoryDto> filtered = new ArrayList<>();
        for (CategoryDto dto : categories) {
            if (dto.getName() == null) continue;
            String nameLower = dto.getName().toLowerCase().trim();
            boolean isSubCat = nameLower.contains("gender-based") || 
                               nameLower.contains("gbv") || 
                               nameLower.contains("sexual harassment") || 
                               nameLower.contains("human rights") || 
                               nameLower.contains("child labour") || 
                               nameLower.contains("child abuse") || 
                               nameLower.contains("violence against children");
            if (!isSubCat) {
                filtered.add(dto);
            }
        }
        this.categories = filtered;
        updateSelectedPosition();
        notifyDataSetChanged();
    }

    public void setSelectedCategory(String categoryName) {
        if (categoryName == null) return;
        this.selectedCategoryName = categoryName;
        updateSelectedPosition();
        notifyDataSetChanged();
    }

    private void updateSelectedPosition() {
        selectedPosition = -1;
        if (selectedCategoryName != null && categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                CategoryDto cat = categories.get(i);
                if (cat.getName() != null && (cat.getName().equalsIgnoreCase(selectedCategoryName) || 
                    cat.getName().toLowerCase().contains(selectedCategoryName.toLowerCase()))) {
                    selectedPosition = i;
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryDto category = categories.get(position);
        holder.bind(category, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding binding;

        public CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CategoryDto category, boolean isSelected) {
            binding.tvCategoryName.setText(category.getName());
            
            // Map icon
            int iconRes = R.drawable.about;
            String name = category.getName().toLowerCase();
            if (name.contains("water")) iconRes = R.drawable.water;
            else if (name.contains("sanit")) iconRes = R.drawable.sanitary;
            else if (name.contains("waste") || name.contains("garb") || name.contains("bin")) iconRes = R.drawable.recycle_bin;
            else if (name.contains("road")) iconRes = R.drawable.road;
            else if (name.contains("elect")) iconRes = R.drawable.electricity;
            else if (name.contains("secu")) iconRes = R.drawable.security;
            else if (name.contains("sensit") || name.contains("gender") || name.contains("abuse") || name.contains("harass")) iconRes = R.drawable.privacy;
            
            binding.ivCategoryIcon.setImageResource(iconRes);
            
            binding.cardCategory.setStrokeWidth(isSelected ? 4 : 0);
            
            binding.getRoot().setOnClickListener(v -> {
                selectedCategoryName = category.getName();
                updateSelectedPosition();
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onCategorySelected(category);
                }
            });
        }
    }
}
