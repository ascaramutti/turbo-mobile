package com.turbo.mobile.ui.owner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.R;
import com.turbo.mobile.ui.TurboUi;
import com.turbo.mobile.ui.notifications.NotificationsFragment;
import com.turbo.mobile.ui.profile.ProfileFragment;

/** Car owner home: top bar, the current tab (a fragment) and the bottom bar. */
public class OwnerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_owner);

        // Bottom bar: each tab shows its own fragment.
        findViewById(R.id.nav_vehicles).setOnClickListener(v -> showTab(R.id.nav_vehicles, new MyVehiclesFragment()));
        findViewById(R.id.nav_bookings).setOnClickListener(v -> showTab(R.id.nav_bookings, new BookingsFragment()));
        findViewById(R.id.nav_earnings).setOnClickListener(v -> showTab(R.id.nav_earnings, new EarningsFragment()));
        findViewById(R.id.nav_alerts).setOnClickListener(v -> showTab(R.id.nav_alerts, new NotificationsFragment()));
        findViewById(R.id.nav_profile).setOnClickListener(v -> showTab(R.id.nav_profile, new ProfileFragment()));

        // Always start on "Vehicles" (also after rotating the phone).
        showTab(R.id.nav_vehicles, new MyVehiclesFragment());
    }

    // Puts the tab's fragment in the content area and marks its item of the bottom bar as selected
    // (the selected item gets the orange colors of bg_turbo_nav_item.xml and color/turbo_nav_item.xml).
    private void showTab(int navId, Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.owner_content, fragment)
                .commit();
        findViewById(R.id.nav_vehicles).setSelected(navId == R.id.nav_vehicles);
        findViewById(R.id.nav_bookings).setSelected(navId == R.id.nav_bookings);
        findViewById(R.id.nav_earnings).setSelected(navId == R.id.nav_earnings);
        findViewById(R.id.nav_alerts).setSelected(navId == R.id.nav_alerts);
        findViewById(R.id.nav_profile).setSelected(navId == R.id.nav_profile);
    }
}
