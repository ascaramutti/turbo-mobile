package com.turbo.mobile.ui.admin;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.ActivityUserAccountBinding;
import com.turbo.mobile.ui.TurboUi;

public class UserAccountActivity extends AppCompatActivity {

    private ActivityUserAccountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);
        TurboUi.setUpAdminBar(this);

        String name = getIntent().getStringExtra(UsersActivity.EXTRA_NAME);
        String role = getIntent().getStringExtra(UsersActivity.EXTRA_ROLE);
        String email = getIntent().getStringExtra(UsersActivity.EXTRA_EMAIL);
        String status = getIntent().getStringExtra(UsersActivity.EXTRA_STATUS);

        if (name == null) {
            name = "Raj Patel";
            role = "Driver";
            email = "raj@mail.com";
            status = "Suspended";
        }

        binding.txtAvatar.setText(initials(name));
        binding.txtName.setText(getString(R.string.userRoleEmailFormat, name, role));
        binding.txtEmail.setText(email);
        StatusChip.apply(binding.chipStatus, status);

        binding.imgBack.setOnClickListener(v -> finish());
        binding.btnActivate.setOnClickListener(v -> changeStatus(true));
        binding.btnDeactivate.setOnClickListener(v -> changeStatus(false));
    }

    // Activates or deactivates the account, then goes back to "Users", which reloads its list.
    // Deactivating a driver also cancels their open bookings (the repository does it).
    private void changeStatus(boolean active) {
        long userId = getIntent().getLongExtra(UsersActivity.EXTRA_USER_ID, 0);
        AppContainer container = ((TurboApp) getApplication()).getContainer();
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<DeactivationResult> result = container.userRepository.setActive(adminId, userId, active);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, active ? R.string.accountActivated : R.string.accountDeactivated,
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    private String initials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
