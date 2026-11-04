package com.turbo.mobile.ui.owner;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

        binding.navVehicles.setOnClickListener(v -> showTab(binding.navVehicles, new MyVehiclesFragment()));

        showTab(binding.navVehicles, new MyVehiclesFragment());
    }

    private void showTab(View tab, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        binding.navVehicles.setSelected(tab == binding.navVehicles);
    }
}
