package com.voiceyanga.citizen.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import com.voiceyanga.citizen.R;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.databinding.ActivityOnboardingBinding;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OnboardingActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OnboardingAdapter adapter = new OnboardingAdapter();
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {}).attach();

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getItemCount() - 1) {
                    binding.btnNext.setText("Get Started");
                } else {
                    binding.btnNext.setText("Next");
                }
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (binding.viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });

        binding.btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        sessionManager.setFirstLaunch(false);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

        private final int[] images = {R.drawable.vote_icon, R.drawable.ic_location_vector, R.drawable.profile};
        private final String[] titles = {"Report Problems", "Track Progress", "Build Community"};
        private final String[] descriptions = {
                "Snap a photo and record a voice note to report issues in your neighborhood.",
                "Get real-time updates as local authorities work to resolve your reported issues.",
                "Support reports from your neighbors and advocate for a better community together."
        };

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.ivImage.setImageResource(images[position]);
            holder.tvTitle.setText(titles[position]);
            holder.tvDescription.setText(descriptions[position]);
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvTitle;
            TextView tvDescription;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivOnboarding);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}
