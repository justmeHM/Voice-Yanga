package com.voiceyanga.citizen.feature.complaints;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.voiceyanga.citizen.data.remote.dto.LocationDto;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private final List<LocationDto> originalList;
    private List<LocationDto> filteredList;
    private final OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(LocationDto location);
    }

    public LocationAdapter(List<LocationDto> locations, OnLocationClickListener listener) {
        this.originalList = new ArrayList<>(locations);
        this.filteredList = new ArrayList<>(locations);
        this.listener = listener;
    }

    public void filter(String query) {
        if (query.isEmpty()) {
            filteredList = new ArrayList<>(originalList);
        } else {
            filteredList = originalList.stream()
                    .filter(l -> l.getDisplayName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationDto location = filteredList.get(position);
        holder.textView.setText(location.getDisplayName());
        holder.itemView.setOnClickListener(v -> listener.onLocationClick(location));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
        }
    }
}
