package com.turbo.mobile.ui.admin;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.R;
import com.turbo.mobile.databinding.ActivityUserAccountBinding;

public class UserAccountActivity extends AppCompatActivity {

    private ActivityUserAccountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        binding.btnActivate.setOnClickListener(v ->
                Toast.makeText(this, "Activate (visual only)", Toast.LENGTH_SHORT).show());
        binding.btnDeactivate.setOnClickListener(v ->
                Toast.makeText(this, "Deactivate (visual only)", Toast.LENGTH_SHORT).show());
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
