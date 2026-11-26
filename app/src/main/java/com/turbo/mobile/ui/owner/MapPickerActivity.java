package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.turbo.mobile.R;
import com.turbo.mobile.databinding.ActivityMapPickerBinding;
import com.turbo.mobile.ui.ScreenSetup;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
        binding.searchButton.setOnClickListener(v -> searchAddress());

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

    private void searchAddress() {
        String text = binding.searchAddress.getText().toString().trim();
        if (text.isEmpty() || map == null) {
            return;
        }

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(text, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng point = new LatLng(address.getLatitude(), address.getLongitude());
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 16));
                return;
            }
        } catch (IOException e) {
            Log.e("MapPickerActivity", "Could not search the address", e);
        }
        Toast.makeText(this, R.string.map_picker_not_found, Toast.LENGTH_SHORT).show();
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
        return String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude);
    }
}