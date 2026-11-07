package com.turbo.mobile.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.databinding.ActivitySplashBinding;
import com.turbo.mobile.ui.auth.AuthViewModel;
import com.turbo.mobile.ui.admin.AdminHomeActivity;
import com.turbo.mobile.ui.driver.DriverHomeActivity;
import com.turbo.mobile.ui.owner.OwnerHomeActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        AuthViewModel viewModel = new AuthViewModel(container.userRepository, container.sessionManager);
        if (!viewModel.isLoggedIn()) {
            return;
        }

        if (viewModel.getRole() == Role.DRIVER) {
            openHome(DriverHomeActivity.class);
        } else if (viewModel.getRole() == Role.CAR_OWNER) {
            openHome(OwnerHomeActivity.class);
        } else if (viewModel.getRole() == Role.ADMIN) {
            openHome(AdminHomeActivity.class);
        }
    }

    private void openHome(Class<?> home) {
        startActivity(new Intent(this, home));
        finish();
    }
}
