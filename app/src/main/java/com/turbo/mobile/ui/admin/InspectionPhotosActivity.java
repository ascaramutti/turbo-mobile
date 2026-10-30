package com.turbo.mobile.ui.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.databinding.ActivityInspectionPhotosBinding;

public class InspectionPhotosActivity extends AppCompatActivity {

    private ActivityInspectionPhotosBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInspectionPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
