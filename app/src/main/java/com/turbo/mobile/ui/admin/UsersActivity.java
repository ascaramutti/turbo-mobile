package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.R;
import com.turbo.mobile.databinding.ActivityUsersBinding;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_ROLE = "EXTRA_ROLE";
    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    public static final String EXTRA_STATUS = "EXTRA_STATUS";

    private ActivityUsersBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UsersAdapter adapter = new UsersAdapter(sampleUsers(), this::onUserClick);
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUsers.setAdapter(adapter);

        binding.btnActivateCancel.setOnClickListener(v ->
                binding.overlayActivate.setVisibility(View.GONE));
        binding.btnActivateConfirm.setOnClickListener(v -> {
            binding.overlayActivate.setVisibility(View.GONE);
            Toast.makeText(this, "Account activated (visual only)", Toast.LENGTH_SHORT).show();
        });
    }

    private void onUserClick(UsersAdapter.UserRow user) {
        if ("Suspended".equals(user.status)) {
            binding.txtActivateMessage.setText(getString(R.string.activateMessageFormat, user.fullName));
            binding.overlayActivate.setVisibility(View.VISIBLE);
        } else {
            openUserAccount(user);
        }
    }

    private void openUserAccount(UsersAdapter.UserRow user) {
        Intent intent = new Intent(UsersActivity.this, UserAccountActivity.class);
        intent.putExtra(EXTRA_NAME, user.fullName);
        intent.putExtra(EXTRA_ROLE, user.role);
        intent.putExtra(EXTRA_EMAIL, user.email);
        intent.putExtra(EXTRA_STATUS, user.status);
        startActivity(intent);
    }

    /** Placeholder list matching the "Users" screen of the PDF. Replaced by real data later. */
    private List<UsersAdapter.UserRow> sampleUsers() {
        List<UsersAdapter.UserRow> users = new ArrayList<>();
        users.add(new UsersAdapter.UserRow("Ana Lopez", "Driver", "ana@mail.com", "Active"));
        users.add(new UsersAdapter.UserRow("Maria Lee", "Owner", "maria@mail.com", "Active"));
        users.add(new UsersAdapter.UserRow("Raj Patel", "Driver", "raj@mail.com", "Suspended"));
        users.add(new UsersAdapter.UserRow("Tom Wu", "Owner", "tom@mail.com", "Deactivated"));
        return users;
    }
}
