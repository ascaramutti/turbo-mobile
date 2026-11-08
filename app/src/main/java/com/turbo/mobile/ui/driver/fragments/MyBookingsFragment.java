package com.turbo.mobile.ui.driver.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.databinding.FragmentMyBookingsBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.driver.BookingDetailActivity;
import com.turbo.mobile.ui.driver.adapters.DriverBookingAdapter;
import com.turbo.mobile.ui.driver.viewmodels.MyBookingsViewModel;

import java.util.List;

public class MyBookingsFragment extends Fragment {

    public static final String ARG_REQUEST_SENT = "request_sent";

    private FragmentMyBookingsBinding binding;
    private MyBookingsViewModel viewModel;
    private DriverBookingAdapter adapter;
    private DriverTab tab = DriverTab.UPCOMING;

    public MyBookingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyBookingsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new MyBookingsViewModel(appContainer.bookingRepository, appContainer.paymentRepository, appContainer.sessionManager);

        adapter = new DriverBookingAdapter(this);
        binding.recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBookings.setAdapter(adapter);

        binding.tabUpcoming.setOnClickListener(v -> selectTab(DriverTab.UPCOMING));
        binding.tabInProgress.setOnClickListener(v -> selectTab(DriverTab.IN_PROGRESS));
        binding.tabHistory.setOnClickListener(v -> selectTab(DriverTab.HISTORY));

        if (getArguments() != null && getArguments().getBoolean(ARG_REQUEST_SENT)) {
            binding.successBanner.setVisibility(View.VISIBLE);
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void openBooking(BookingListItem booking) {
        Intent intent = new Intent(requireContext(), BookingDetailActivity.class);
        intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
        startActivity(intent);
    }

    private void selectTab(DriverTab selected) {
        tab = selected;
        binding.successBanner.setVisibility(View.GONE);
        loadBookings();
    }

    private void loadBookings() {
        binding.tabUpcoming.setSelected(tab == DriverTab.UPCOMING);
        binding.tabInProgress.setSelected(tab == DriverTab.IN_PROGRESS);
        binding.tabHistory.setSelected(tab == DriverTab.HISTORY);
        binding.errorBanner.setVisibility(View.GONE);

        showSummary();

        List<BookingListItem> bookings = viewModel.getBookings(tab);
        adapter.setItems(bookings);
        binding.emptyState.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSummary() {
        binding.summary.setVisibility(View.GONE);
        if (tab == DriverTab.HISTORY) {
            return;
        }

        Result<WeeklyHours> hoursResult = viewModel.loadWeeklyHours();
        if (hoursResult instanceof Result.Error) {
            showError(((Result.Error<WeeklyHours>) hoursResult).code);
            return;
        }
        Result<DriverSpending> spendingResult = viewModel.loadSpending();
        if (spendingResult instanceof Result.Error) {
            showError(((Result.Error<DriverSpending>) spendingResult).code);
            return;
        }
        WeeklyHours hours = ((Result.Success<WeeklyHours>) hoursResult).data;
        DriverSpending spending = ((Result.Success<DriverSpending>) spendingResult).data;

        binding.weekHours.setText(getString(R.string.my_bookings_week_hours, (int) hours.getBookedHours(), hours.getLimitHours()));
        binding.monthSpent.setText(DisplayFormat.money(spending.getMonthCents()));
        binding.totalSpent.setText(DisplayFormat.money(spending.getTotalCents()));
        binding.completedShifts.setText(String.valueOf(spending.getCompletedCount()));
        binding.summary.setVisibility(View.VISIBLE);
    }

    private void showError(RepoError code) {
        int message = R.string.error_unexpected;
        if (code == BookingError.NOT_FOUND || code == PaymentError.NOT_FOUND) {
            message = R.string.error_not_found;
        }
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }
}
