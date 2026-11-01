package com.turbo.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.databinding.FragmentProfileBinding;
import com.turbo.mobile.ui.TurboUi;
import com.turbo.mobile.ui.driver.MyDocumentsActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cardDriverDocuments.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MyDocumentsActivity.class));
        });

        binding.btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> {
            TurboUi.showConfirmDialog(requireActivity(),
                    getString(R.string.log_out_title),
                    getString(R.string.log_out_message),
                    getString(R.string.cancel),
                    getString(R.string.txtLogout),
                    () -> TurboUi.logOut(requireActivity()));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        if (!isAdded()) return;
        AppContainer container = ((TurboApp) requireActivity().getApplication()).getContainer();
        long userId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<UserProfile> result = container.userRepository.getProfile(userId);
            if (isAdded() && result instanceof Result.Success) {
                UserProfile profile = ((Result.Success<UserProfile>) result).data;
                if (profile != null) {
                    requireActivity().runOnUiThread(() -> bindProfile(profile));
                }
            }
        });
    }

    private void bindProfile(UserProfile profile) {
        if (binding == null) return;

        String initials = "";
        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
            initials += profile.getFirstName().charAt(0);
        }
        if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
            initials += profile.getLastName().charAt(0);
        }
        binding.profileAvatar.setText(initials.toUpperCase());

        binding.profileName.setText(profile.getFirstName() + " " + profile.getLastName());
        binding.profileEmail.setText(profile.getEmail());

        if (profile.getRole() == Role.DRIVER) {
            String roleDesc = "Driver";
            if (profile.getLicenseClass() != null) {
                roleDesc += " · " + profile.getLicenseClass().name().replace('_', ' ');
            }
            binding.profileRole.setText(roleDesc);

            boolean verified = profile.getDriverState() == DriverState.VERIFIED;
            binding.bannerVerified.setVisibility(verified ? View.VISIBLE : View.GONE);
            binding.cardDriverDocuments.setVisibility(View.VISIBLE);
            binding.cardOwnerAccount.setVisibility(View.GONE);
        } else {
            binding.profileRole.setText("Car Owner");
            binding.bannerVerified.setVisibility(View.GONE);
            binding.cardDriverDocuments.setVisibility(View.GONE);
            binding.cardOwnerAccount.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
