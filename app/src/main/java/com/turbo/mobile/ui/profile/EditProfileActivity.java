package com.turbo.mobile.ui.profile;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.databinding.ActivityEditProfileBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.util.ImageStorage;
import com.turbo.mobile.util.Validators;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ProfileViewModel viewModel;
    private ImageStorage imageStorage;
    private Uri selectedPhotoUri;

    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    binding.avatarImage.setImageURI(uri);
                    binding.avatarImage.setVisibility(View.VISIBLE);
                    binding.avatarInitials.setVisibility(View.GONE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new ProfileViewModel(appContainer.userRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        binding.btnChangePhoto.setOnClickListener(v -> pickPhoto.launch("image/*"));
        binding.btnSave.setOnClickListener(v -> saveProfile());

        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        Result<UserProfile> result = viewModel.loadProfile();
        if (result instanceof Result.Success) {
            UserProfile profile = ((Result.Success<UserProfile>) result).data;
            binding.firstName.setText(profile.getFirstName());
            binding.lastName.setText(profile.getLastName());
            binding.phone.setText(profile.getPhone());

            String initials = "";
            if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
                initials += profile.getFirstName().charAt(0);
            }
            if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
                initials += profile.getLastName().charAt(0);
            }
            binding.avatarInitials.setText(initials.toUpperCase());

            Bitmap photo = null;
            if (profile.getProfilePhotoPath() != null && !profile.getProfilePhotoPath().isEmpty()) {
                photo = imageStorage.load(profile.getProfilePhotoPath(), 400);
            }

            if (photo != null) {
                binding.avatarImage.setImageBitmap(photo);
                binding.avatarImage.setVisibility(View.VISIBLE);
                binding.avatarInitials.setVisibility(View.GONE);
            } else {
                binding.avatarImage.setVisibility(View.GONE);
                binding.avatarInitials.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveProfile() {
        binding.errorBanner.setVisibility(View.GONE);

        String firstName = binding.firstName.getText() != null ? binding.firstName.getText().toString().trim() : "";
        String lastName = binding.lastName.getText() != null ? binding.lastName.getText().toString().trim() : "";
        String phone = binding.phone.getText() != null ? binding.phone.getText().toString().trim() : "";

        if (Validators.isBlank(firstName)) {
            binding.errorBanner.setText(R.string.edit_profile_error_first_name);
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        if (Validators.isBlank(lastName)) {
            binding.errorBanner.setText(R.string.edit_profile_error_last_name);
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        if (!Validators.isPhone(phone)) {
            binding.errorBanner.setText(R.string.edit_profile_error_phone);
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        Result<Void> result = viewModel.updateProfile(firstName, lastName, phone, selectedPhotoUri);
        if (result instanceof Result.Error) {
            RepoError code = ((Result.Error<Void>) result).code;
            if (code == UserError.INVALID_INPUT) {
                binding.errorBanner.setText(R.string.edit_profile_error_invalid_input);
            } else if (code == UserError.FILE_ERROR) {
                binding.errorBanner.setText(R.string.edit_profile_error_photo);
            } else {
                binding.errorBanner.setText(R.string.error_unexpected);
            }
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        Toast.makeText(this, R.string.edit_profile_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
