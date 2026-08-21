package com.voiceyanga.citizen.feature.home;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.voiceyanga.citizen.R;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}