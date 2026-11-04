package com.turbo.mobile.ui.owner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.databinding.ActivityOwnerHomeBinding;
import com.turbo.mobile.ui.ScreenSetup;

public class OwnerHomeActivity extends AppCompatActivity {

    private ActivityOwnerHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);
    }
}
