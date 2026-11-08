package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.databinding.ActivityDriverHomeBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.fragments.MyBookingsFragment;
import com.turbo.mobile.ui.driver.fragments.SearchFragment;

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
        binding.navBookings.setOnClickListener(v -> showTab(binding.navBookings, new MyBookingsFragment()));

        showTab(binding.navSearch, new SearchFragment());
    }

    private void showTab(View tab, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        binding.navSearch.setSelected(tab == binding.navSearch);
        binding.navBookings.setSelected(tab == binding.navBookings);
    }

    public void showRequestSent() {
        Bundle arguments = new Bundle();
        arguments.putBoolean(MyBookingsFragment.ARG_REQUEST_SENT, true);
        MyBookingsFragment fragment = new MyBookingsFragment();
        fragment.setArguments(arguments);
        showTab(binding.navBookings, fragment);
    }
}
