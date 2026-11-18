package com.turbo.mobile.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.databinding.ItemNotificationBinding;
import com.turbo.mobile.ui.DisplayFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter displaying notifications with unread badge, tap to mark read, and delete button.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface OnNotificationActionListener {
        void onNotificationClick(NotificationItem item);

        void onDeleteClick(NotificationItem item);
    }

    private final OnNotificationActionListener listener;
    private List<NotificationItem> items = new ArrayList<>();

    public NotificationsAdapter(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.binding.notificationTitle.setText(item.getTitle());
        holder.binding.notificationMessage.setText(item.getMessage());
        holder.binding.notificationDate.setText(DisplayFormat.date(item.getCreatedAt()));
        holder.binding.chipUnread.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(item);
            }
        });

        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final ItemNotificationBinding binding;

        NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
