package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.databinding.ActivityAdminHomeBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.SplashActivity;
import com.turbo.mobile.ui.admin.fragments.PendingDocumentsFragment;

public class AdminHomeActivity extends AppCompatActivity {

    private ActivityAdminHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.avatar.setOnClickListener(v -> confirmLogout());

        binding.navDocuments.setOnClickListener(v -> showTab(binding.navDocuments, new PendingDocumentsFragment()));
        binding.navBookings.setOnClickListener(v -> selectTab(binding.navBookings));
        binding.navUsers.setOnClickListener(v -> selectTab(binding.navUsers));

        showTab(binding.navDocuments, new PendingDocumentsFragment());
    }

    public void showTab(View tab, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        selectTab(tab);
    }

    private void selectTab(View tab) {
        binding.navDocuments.setSelected(tab == binding.navDocuments);
        binding.navBookings.setSelected(tab == binding.navBookings);
        binding.navUsers.setSelected(tab == binding.navUsers);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout_confirm, (dialog, which) -> logout())
                .setNegativeButton(R.string.logout_cancel, null)
                .show();
    }

    private void logout() {
        AppContainer container = ((TurboApp) getApplication()).getContainer();
        container.sessionManager.clear();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
