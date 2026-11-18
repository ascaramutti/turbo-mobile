package com.turbo.mobile.ui.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.databinding.ItemAdminUserBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter displaying Driver and Car Owner users with an active toggle switch.
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserToggleListener {
        void onUserToggle(UserListItem user, boolean active);
    }

    private final OnUserToggleListener listener;
    private List<UserListItem> items = new ArrayList<>();

    public UsersAdapter(OnUserToggleListener listener) {
        this.listener = listener;
    }

    public void setItems(List<UserListItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListItem user = items.get(position);
        Context context = holder.binding.getRoot().getContext();

        String fullName = user.getFullName() != null ? user.getFullName() : "";
        String initial = !fullName.isEmpty() ? String.valueOf(fullName.charAt(0)).toUpperCase() : "";
        holder.binding.userInitial.setText(initial);
        holder.binding.userName.setText(fullName);

        String roleLabel = user.getRole() == Role.DRIVER
                ? context.getString(R.string.users_role_driver)
                : context.getString(R.string.users_role_owner);
        holder.binding.userSubtitle.setText(
                context.getString(R.string.users_item_subtitle, roleLabel, user.getEmail()));

        holder.binding.userActiveSwitch.setOnCheckedChangeListener(null);
        holder.binding.userActiveSwitch.setChecked(user.isActive());
        holder.binding.userActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onUserToggle(user, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminUserBinding binding;

        UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
