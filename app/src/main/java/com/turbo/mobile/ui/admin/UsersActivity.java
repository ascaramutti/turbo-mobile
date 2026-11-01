package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.databinding.ActivityUsersBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

public class UsersActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_ROLE = "EXTRA_ROLE";
    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    public static final String EXTRA_STATUS = "EXTRA_STATUS";

    private ActivityUsersBinding binding;
    private AppContainer container;
    private UserListItem userToActivate; // the user of the "Activate account?" dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);
        TurboUi.setUpAdminBar(this);

        container = ((TurboApp) getApplication()).getContainer();
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));

        binding.btnActivateCancel.setOnClickListener(v ->
                binding.overlayActivate.setVisibility(View.GONE));
        binding.btnActivateConfirm.setOnClickListener(v -> {
            binding.overlayActivate.setVisibility(View.GONE);
            activate(userToActivate);
        });
    }

    // Also runs when coming back from "User account", so a deactivated user shows "Inactive".
    @Override
    protected void onResume() {
        super.onResume();
        showUsers();
    }

    private void showUsers() {
        container.executor.execute(() -> {
            List<UserListItem> users = container.userRepository.getUsers(null, null);
            runOnUiThread(() -> binding.recyclerUsers.setAdapter(new UsersAdapter(users, user -> onUserClick(user))));
        });
    }

    // An inactive user gets the "Activate account?" dialog; an active one opens their account.
    private void onUserClick(UserListItem user) {
        if (!user.isActive()) {
            userToActivate = user;
            binding.txtActivateMessage.setText(getString(R.string.activateMessageFormat, user.getFullName()));
            binding.overlayActivate.setVisibility(View.VISIBLE);
        } else {
            openUserAccount(user);
        }
    }

    private void activate(UserListItem user) {
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<DeactivationResult> result = container.userRepository.setActive(adminId, user.getId(), true);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.accountActivated, Toast.LENGTH_SHORT).show();
                    showUsers();
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    private void openUserAccount(UserListItem user) {
        Intent intent = new Intent(UsersActivity.this, UserAccountActivity.class);
        intent.putExtra(EXTRA_USER_ID, user.getId());
        intent.putExtra(EXTRA_NAME, user.getFullName());
        intent.putExtra(EXTRA_ROLE, UsersAdapter.roleName(this, user.getRole()));
        intent.putExtra(EXTRA_EMAIL, user.getEmail());
        intent.putExtra(EXTRA_STATUS, UsersAdapter.statusName(this, user));
        startActivity(intent);
    }
}
