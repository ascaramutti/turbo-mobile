package com.turbo.mobile.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.databinding.FragmentProfileBinding;
import com.turbo.mobile.ui.SplashActivity;
import com.turbo.mobile.util.ImageStorage;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private ImageStorage imageStorage;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new ProfileViewModel(appContainer.userRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        binding.cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        binding.cardMyDocuments.setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.profile_documents, Toast.LENGTH_SHORT).show();
        });

        binding.cardLogout.setOnClickListener(v -> confirmLogout());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadProfileData() {
        if (binding == null || viewModel == null) return;

        Result<UserProfile> result = viewModel.loadProfile();
        if (!(result instanceof Result.Success)) {
            return;
        }

        UserProfile profile = ((Result.Success<UserProfile>) result).data;

        // Nome
        String fullName = profile.getFirstName() + " " + profile.getLastName();
        binding.profileName.setText(fullName);

        // Subtítulo e opções específicas de cada papel
        if (profile.getRole() == Role.DRIVER) {
            String license = profile.getLicenseClass() == LicenseClass.CLASS_4 ? "Class 4" : "Class 5";
            binding.profileSubtitle.setText(profile.getEmail() + " · Driver (" + license + ")");
            binding.cardMyDocuments.setVisibility(View.VISIBLE);

            // Badge do motorista
            binding.badgeStatus.setVisibility(View.VISIBLE);
            if (profile.getDriverState() == DriverState.VERIFIED || profile.getDriverState() == DriverState.VERIFIED_RENEW) {
                binding.badgeStatus.setText(R.string.profile_status_verified);
            } else if (profile.getDriverState() == DriverState.DOCUMENT_REJECTED) {
                binding.badgeStatus.setText(R.string.profile_status_rejected);
            } else {
                binding.badgeStatus.setText(R.string.profile_status_pending);
            }
        } else {
            binding.profileSubtitle.setText(profile.getEmail() + " · Car Owner");
            binding.cardMyDocuments.setVisibility(View.GONE);
            binding.badgeStatus.setVisibility(View.GONE);
        }

        // Avatar: foto ou iniciais
        Bitmap photo = null;
        if (profile.getProfilePhotoPath() != null && !profile.getProfilePhotoPath().isEmpty()) {
            photo = imageStorage.load(profile.getProfilePhotoPath(), 400);
        }

        if (photo != null) {
            binding.avatarImage.setImageBitmap(photo);
            binding.avatarImage.setVisibility(View.VISIBLE);
            binding.avatarInitials.setVisibility(View.GONE);
        } else {
            String initials = "";
            if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
                initials += profile.getFirstName().charAt(0);
            }
            if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
                initials += profile.getLastName().charAt(0);
            }
            binding.avatarInitials.setText(initials.toUpperCase());
            binding.avatarImage.setVisibility(View.GONE);
            binding.avatarInitials.setVisibility(View.VISIBLE);
        }

        // Avaliação (rating)
        if (profile.getRatingAvg() != null && profile.getRatingCount() > 0) {
            binding.profileRating.setText(String.format("★ %.1f (%d)", profile.getRatingAvg(), profile.getRatingCount()));
            binding.profileRating.setVisibility(View.VISIBLE);
        } else {
            binding.profileRating.setVisibility(View.GONE);
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout_confirm, (dialog, which) -> logout())
                .setNegativeButton(R.string.logout_cancel, null)
                .show();
    }

    private void logout() {
        viewModel.logout();
        Intent intent = new Intent(requireContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
