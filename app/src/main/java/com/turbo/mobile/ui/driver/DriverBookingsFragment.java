package com.turbo.mobile.ui.driver;

import android.content.Intent;
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
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.databinding.FragmentDriverBookingsBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

public class DriverBookingsFragment extends Fragment {

    private FragmentDriverBookingsBinding binding;
    private AppContainer container;
    private long driverId;
    private int currentTab = R.id.tab_upcoming;
    private DriverBookingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();

        adapter = new DriverBookingsAdapter(booking -> {
            Intent intent = new Intent(requireContext(), DriverBookingDetailActivity.class);
            intent.putExtra(DriverBookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
            startActivity(intent);
        });

        binding.recyclerDriverBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDriverBookings.setAdapter(adapter);

        binding.tabUpcoming.setOnClickListener(v -> selectTab(R.id.tab_upcoming));
        binding.tabInProgress.setOnClickListener(v -> selectTab(R.id.tab_in_progress));
        binding.tabHistory.setOnClickListener(v -> selectTab(R.id.tab_history));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Reload every time the screen is visible again (e.g. after paying or cancelling in W-09).
    @Override
    public void onResume() {
        super.onResume();
        selectTab(currentTab);
    }

    private void selectTab(int tabId) {
        if (binding == null) return;
        currentTab = tabId;
        binding.tabUpcoming.setSelected(tabId == R.id.tab_upcoming);
        binding.tabInProgress.setSelected(tabId == R.id.tab_in_progress);
        binding.tabHistory.setSelected(tabId == R.id.tab_history);
        loadBookings();
    }

    // Reads the bookings of the selected tab and the totals at the top.
    private void loadBookings() {
        int tabId = currentTab;
        DriverTab tab = repositoryTab(tabId);
        container.executor.execute(() -> {
            List<BookingListItem> bookings = container.bookingRepository.getDriverBookings(driverId, tab);
            Result<WeeklyHours> hours = container.bookingRepository.getWeeklyHours(driverId);
            Result<DriverSpending> spending = container.paymentRepository.getDriverSpending(driverId);
            TurboUi.runOnMainThread(this, () -> {
                showStats(hours, spending, tabId);
                showBookings(bookings, tabId);
            });
        });
    }

    // The repository's name of each tab.
    private DriverTab repositoryTab(int tabId) {
        if (tabId == R.id.tab_upcoming) {
            return DriverTab.UPCOMING;
        }
        if (tabId == R.id.tab_in_progress) {
            return DriverTab.IN_PROGRESS;
        }
        return DriverTab.HISTORY;
    }

    // "12 of 24 h", "CAD 160.00", "CAD 640.00" and "14". History doesn't show them.
    private void showStats(Result<WeeklyHours> hoursResult, Result<DriverSpending> spendingResult, int tabId) {
        if (binding == null) return;
        if (tabId == R.id.tab_history || hoursResult instanceof Result.Error || spendingResult instanceof Result.Error) {
            binding.stats.setVisibility(View.GONE);
            return;
        }
        WeeklyHours hours = ((Result.Success<WeeklyHours>) hoursResult).data;
        DriverSpending spending = ((Result.Success<DriverSpending>) spendingResult).data;
        // (int) drops the decimals: shifts are 4 or 8 hours, so the total is a whole number.
        binding.weekHours.setText(getString(R.string.hours_of_limit, (int) hours.getBookedHours(), hours.getLimitHours()));
        binding.thisMonth.setText(TurboUi.money(spending.getMonthCents()));
        binding.totalSpent.setText(TurboUi.money(spending.getTotalCents()));
        binding.completedShifts.setText(String.valueOf(spending.getCompletedCount()));
        binding.stats.setVisibility(View.VISIBLE);
    }

    private void showBookings(List<BookingListItem> bookings, int tabId) {
        if (binding == null) return;
        adapter.setItems(bookings, tabId);
        binding.bookingsEmpty.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
