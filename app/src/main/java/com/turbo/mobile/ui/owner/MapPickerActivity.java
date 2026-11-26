package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.turbo.mobile.databinding.ActivityMapPickerBinding;
import com.turbo.mobile.ui.ScreenSetup;

import java.io.IOException;
import java.util.List;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private ActivityMapPickerBinding binding;
    private GoogleMap map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());
        binding.confirmLocation.setOnClickListener(v -> confirmLocation());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(binding.map.getId());
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        LatLng start = new LatLng(49.2827, -123.1207);
        if (getIntent().hasExtra(EXTRA_LATITUDE)) {
            start = new LatLng(getIntent().getDoubleExtra(EXTRA_LATITUDE, 0),
                    getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0));
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15));
    }

    private void confirmLocation() {
        if (map == null) {
            return;
        }

        LatLng center = map.getCameraPosition().target;
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, center.latitude);
        result.putExtra(EXTRA_LONGITUDE, center.longitude);
        result.putExtra(EXTRA_ADDRESS, findAddress(center));
        setResult(RESULT_OK, result);
        finish();
    }

    private String findAddress(LatLng point) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }

        } catch (IOException e) {
            Log.e("MapPickerActivity", "Could not find the address", e);
        }
        return "";
    }
}