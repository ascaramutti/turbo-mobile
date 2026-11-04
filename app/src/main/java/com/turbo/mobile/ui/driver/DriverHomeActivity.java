package com.turbo.mobile.ui.driver;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.databinding.ActivityDriverHomeBinding;
import com.turbo.mobile.ui.ScreenSetup;

public class DriverHomeActivity extends AppCompatActivity {

    private ActivityDriverHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);
    }
}
