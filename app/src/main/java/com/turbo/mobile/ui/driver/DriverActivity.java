package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.R;
import com.turbo.mobile.ui.TurboUi;

/** Driver home: top bar, the current tab (a fragment) and the bottom bar. */
public class DriverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_driver);

        // Bottom bar: each tab shows its own fragment. Alerts and Profile don't exist yet.
        findViewById(R.id.nav_search).setOnClickListener(v -> showTab(R.id.nav_search, new SearchFragment()));
        findViewById(R.id.nav_bookings).setOnClickListener(v -> showTab(R.id.nav_bookings, new DriverBookingsFragment()));
        findViewById(R.id.nav_alerts).setOnClickListener(v -> comingSoon());
        findViewById(R.id.nav_profile).setOnClickListener(v -> comingSoon());

        // Always start on "Search" (also after rotating the phone).
        showTab(R.id.nav_search, new SearchFragment());
    }

    // Puts the tab's fragment in the content area and marks its item of the bottom bar as selected.
    private void showTab(int navId, Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.driver_content, fragment)
                .commit();
        findViewById(R.id.nav_search).setSelected(navId == R.id.nav_search);
        findViewById(R.id.nav_bookings).setSelected(navId == R.id.nav_bookings);
    }

    private void comingSoon() {
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }
}
