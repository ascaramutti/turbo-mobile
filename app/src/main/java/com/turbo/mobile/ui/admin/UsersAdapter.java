package com.turbo.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.databinding.ItemUserBinding;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserRow user);
    }

    private final List<UserRow> users;
    private final OnUserClickListener listener;

    public UsersAdapter(List<UserRow> users, OnUserClickListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserRow user = users.get(position);
        holder.binding.txtName.setText(user.fullName);
        holder.binding.txtRoleEmail.setText(holder.binding.getRoot().getContext()
                .getString(R.string.userRoleEmailFormat, user.role, user.email));
        StatusChip.apply(holder.binding.chipStatus, user.status);
        holder.binding.btnViewUser.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /** Simple display model for the users list. Replaced by the real model when we connect the data. */
    public static class UserRow {
        public final String fullName;
        public final String role;
        public final String email;
        public final String status;

        public UserRow(String fullName, String role, String email, String status) {
            this.fullName = fullName;
            this.role = role;
            this.email = email;
            this.status = status;
        }
    }
}
