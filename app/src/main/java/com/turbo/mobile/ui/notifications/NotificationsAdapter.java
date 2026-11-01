package com.turbo.mobile.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.databinding.ItemNotificationBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
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
        holder.binding.notificationTime.setText(TurboUi.dayAndDate(item.getCreatedAt()));

        if (item.isRead()) {
            holder.binding.notificationChip.setVisibility(View.GONE);
            holder.binding.notificationTitle.setTextColor(holder.itemView.getContext().getColor(R.color.turbo_text_secondary));
        } else {
            holder.binding.notificationChip.setVisibility(View.VISIBLE);
            holder.binding.notificationChip.setText(R.string.chipUnread);
            holder.binding.notificationChip.setBackgroundResource(R.drawable.bg_turbo_chip);
            holder.binding.notificationChip.setTextColor(holder.itemView.getContext().getColor(R.color.turbo_warning));
            holder.binding.notificationTitle.setTextColor(holder.itemView.getContext().getColor(R.color.turbo_text_primary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(item);
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
