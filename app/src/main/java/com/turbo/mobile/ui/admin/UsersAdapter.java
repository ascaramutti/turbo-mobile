package com.turbo.mobile.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.databinding.ItemUserBinding;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserListItem user);
    }

    private final List<UserListItem> users;
    private final OnUserClickListener listener;

    public UsersAdapter(List<UserListItem> users, OnUserClickListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
    }

    // "Driver" or "Car Owner" (the users list never includes admins).
    static String roleName(Context context, Role role) {
        return context.getString(role == Role.DRIVER ? R.string.txtRoleDriver : R.string.txtRoleOwner);
    }

    // "Active" or "Inactive"
    static String statusName(Context context, UserListItem user) {
        return context.getString(user.isActive() ? R.string.chip_active : R.string.chip_inactive);
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
        UserListItem user = users.get(position);
        Context context = holder.binding.getRoot().getContext();
        holder.binding.txtName.setText(user.getFullName());
        holder.binding.txtRoleEmail.setText(context.getString(R.string.userRoleEmailFormat,
                roleName(context, user.getRole()), user.getEmail()));
        StatusChip.apply(holder.binding.chipStatus, statusName(context, user));
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
}
