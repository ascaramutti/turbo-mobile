package com.turbo.mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.NotificationItem;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.databinding.FragmentNotificationsBinding;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppContainer container = ((TurboApp) requireActivity().getApplication()).getContainer();
        Role role = container.sessionManager.getRole();
        if (role == Role.DRIVER) {
            binding.notificationsSubtitle.setText(R.string.notificationsSubtitleDriver);
        } else {
            binding.notificationsSubtitle.setText(R.string.notificationsSubtitleOwner);
        }

        adapter = new NotificationsAdapter(item -> {
            if (!item.isRead()) {
                long userId = container.sessionManager.getUserId();
                container.executor.execute(() -> {
                    container.notificationRepository.markAsRead(userId, item.getId());
                    loadNotifications();
                });
            }
        });

        binding.recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerNotifications.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        if (!isAdded()) return;
        AppContainer container = ((TurboApp) requireActivity().getApplication()).getContainer();
        long userId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            List<NotificationItem> list = container.notificationRepository.getNotifications(userId);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (list == null || list.isEmpty()) {
                        binding.emptyNotifications.setVisibility(View.VISIBLE);
                        binding.recyclerNotifications.setVisibility(View.GONE);
                    } else {
                        binding.emptyNotifications.setVisibility(View.GONE);
                        binding.recyclerNotifications.setVisibility(View.VISIBLE);
                        adapter.setItems(list);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
