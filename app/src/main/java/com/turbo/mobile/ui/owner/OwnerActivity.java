package com.turbo.mobile.ui.owner;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserProfile;

public class OwnerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Wireframes are light-only: keep dark system bar icons even when the phone is in dark mode.
        SystemBarStyle light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(this, light, light);
        setContentView(R.layout.activity_owner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.owner_content, new MyVehiclesFragment())
                    .commit();
        }
        findViewById(R.id.nav_vehicles).setSelected(true);
        for (int id : new int[]{R.id.nav_bookings, R.id.nav_earnings, R.id.nav_alerts, R.id.nav_profile}) {
            findViewById(id).setOnClickListener(v ->
                    Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show());
        }
        loadInitials();
    }

    private void loadInitials() {
        AppContainer container = ((TurboApp) getApplication()).getContainer();
        long userId = container.sessionManager.getUserId();
        TextView avatar = findViewById(R.id.avatar);
        container.executor.execute(() -> {
            Result<UserProfile> result = container.userRepository.getProfile(userId);
            if (result instanceof Result.Success) {
                UserProfile profile = ((Result.Success<UserProfile>) result).data;
                String initials = "" + profile.getFirstName().charAt(0) + profile.getLastName().charAt(0);
                runOnUiThread(() -> avatar.setText(initials));
            }
        });
    }
}
