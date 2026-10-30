package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.databinding.ActivityBookingDetailBinding;

public class BookingDetailActivity extends AppCompatActivity {

    private ActivityBookingDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnViewInspectionPhotos.setOnClickListener(v ->
                startActivity(new Intent(this, InspectionPhotosActivity.class)));
    }
}
