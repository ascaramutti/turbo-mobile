package com.turbo.mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentNotificationsBinding;

import java.util.List;

/**
 * Fragment displaying user alerts and reminders (W-03).
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificationsAdapter adapter;

    public NotificationsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new NotificationsViewModel(appContainer.notificationRepository, appContainer.sessionManager);

        adapter = new NotificationsAdapter(new NotificationsAdapter.OnNotificationActionListener() {
            @Override
            public void onNotificationClick(NotificationItem item) {
                if (!item.isRead()) {
                    markAsRead(item);
                }
            }

            @Override
            public void onDeleteClick(NotificationItem item) {
                deleteNotification(item);
            }
        });
        binding.recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerNotifications.setAdapter(adapter);

        binding.btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadNotifications() {
        if (binding == null || viewModel == null) {
            return;
        }
        List<NotificationItem> list = viewModel.getNotifications();
        adapter.setItems(list);
        binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnMarkAllRead.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void markAsRead(NotificationItem item) {
        binding.errorBanner.setVisibility(View.GONE);
        Result<Void> result = viewModel.markAsRead(item.getId());
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        loadNotifications();
    }

    private void markAllAsRead() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<Void> result = viewModel.markAllAsRead();
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        loadNotifications();
    }

    private void deleteNotification(NotificationItem item) {
        binding.errorBanner.setVisibility(View.GONE);
        Result<Void> result = viewModel.delete(item.getId());
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        loadNotifications();
    }
}
