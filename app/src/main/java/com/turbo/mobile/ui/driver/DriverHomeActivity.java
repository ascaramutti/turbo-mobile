package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

        binding.navSearch.setOnClickListener(v -> showTab(binding.navSearch, new SearchFragment()));

        showTab(binding.navSearch, new SearchFragment());
    }

    private void showTab(View tab, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        binding.navSearch.setSelected(tab == binding.navSearch);
    }
}
