package com.turbo.mobile.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.databinding.ActivityEditProfileBinding;
import com.turbo.mobile.ui.TurboUi;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TurboUi.fitSystemBars(this);
        TurboUi.showBackButton(this);

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        long userId = container.sessionManager.getUserId();

        loadProfile(container, userId);

        binding.btnSave.setOnClickListener(v -> saveProfile(container, userId));
    }

    private void loadProfile(AppContainer container, long userId) {
        container.executor.execute(() -> {
            Result<UserProfile> result = container.userRepository.getProfile(userId);
            if (result instanceof Result.Success) {
                UserProfile profile = ((Result.Success<UserProfile>) result).data;
                if (profile != null) {
                    runOnUiThread(() -> {
                        String initials = "";
                        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
                            initials += profile.getFirstName().charAt(0);
                        }
                        if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
                            initials += profile.getLastName().charAt(0);
                        }
                        binding.profileAvatar.setText(initials.toUpperCase());
                        binding.profileDisplayName.setText(profile.getFirstName() + " " + profile.getLastName());
                        binding.profileRole.setText(profile.getRole() == Role.DRIVER ? "Driver" : "Car Owner");

                        binding.inputFirstName.setText(profile.getFirstName());
                        binding.inputLastName.setText(profile.getLastName());
                        binding.inputPhone.setText(profile.getPhone());
                    });
                }
            }
        });
    }

    private void saveProfile(AppContainer container, long userId) {
        String firstName = binding.inputFirstName.getText() != null ? binding.inputFirstName.getText().toString().trim() : "";
        String lastName = binding.inputLastName.getText() != null ? binding.inputLastName.getText().toString().trim() : "";
        String phone = binding.inputPhone.getText() != null ? binding.inputPhone.getText().toString().trim() : "";

        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
            binding.bannerError.setText("Please fill in all required fields.");
            binding.bannerError.setVisibility(View.VISIBLE);
            return;
        }

        binding.bannerError.setVisibility(View.GONE);
        binding.btnSave.setEnabled(false);

        container.executor.execute(() -> {
            Result<Void> result = container.userRepository.updateProfile(userId, firstName, lastName, phone, null);
            runOnUiThread(() -> {
                binding.btnSave.setEnabled(true);
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.profileSavedToast, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = ((Result.Error<Void>) result).message;
                    binding.bannerError.setText(msg != null ? msg : "An error occurred");
                    binding.bannerError.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
