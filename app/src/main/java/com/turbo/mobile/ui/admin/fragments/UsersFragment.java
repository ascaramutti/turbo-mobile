package com.turbo.mobile.ui.admin.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.databinding.FragmentUsersBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.admin.adapters.UsersAdapter;
import com.turbo.mobile.ui.admin.viewmodels.UsersViewModel;

import java.util.List;

/**
 * Admin fragment displaying Driver and Car Owner users with search, role filter, and activation toggle (W-27).
 */
public class UsersFragment extends Fragment {

    private FragmentUsersBinding binding;
    private UsersViewModel viewModel;
    private UsersAdapter adapter;
    private Role selectedRole = null;

    public UsersFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new UsersViewModel(appContainer.userRepository, appContainer.sessionManager);

        adapter = new UsersAdapter(this::onUserToggle);
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(adapter);

        setUpSearchInput();
        setUpRoleSpinner();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpSearchInput() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadUsers();
            }
        });
    }

    private void setUpRoleSpinner() {
        String[] options = {
                getString(R.string.users_role_all),
                getString(R.string.users_role_driver),
                getString(R.string.users_role_owner)
        };
        ScreenSetup.fillSpinner(binding.roleSpinner, options);

        binding.roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    selectedRole = Role.DRIVER;
                } else if (position == 2) {
                    selectedRole = Role.CAR_OWNER;
                } else {
                    selectedRole = null;
                }
                loadUsers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUsers() {
        if (binding == null || viewModel == null) {
            return;
        }
        String query = binding.searchInput.getText() != null
                ? binding.searchInput.getText().toString().trim()
                : "";
        List<UserListItem> users = viewModel.search(query, selectedRole);
        adapter.setItems(users);
        binding.emptyState.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onUserToggle(UserListItem user, boolean active) {
        if (!active) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.users_deactivate_title, user.getFullName()))
                    .setMessage(R.string.users_deactivate_message)
                    .setPositiveButton(R.string.users_deactivate_confirm, (dialog, which) -> applyActiveChange(user, false))
                    .setNegativeButton(R.string.logout_cancel, (dialog, which) -> loadUsers())
                    .setOnCancelListener(dialog -> loadUsers())
                    .show();
        } else {
            applyActiveChange(user, true);
        }
    }

    private void applyActiveChange(UserListItem user, boolean active) {
        binding.errorBanner.setVisibility(View.GONE);
        Result<DeactivationResult> result = viewModel.setActive(user.getId(), active);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<DeactivationResult>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            loadUsers();
            return;
        }

        int messageRes = active ? R.string.users_activated_toast : R.string.users_deactivated_toast;
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        loadUsers();
    }
}
