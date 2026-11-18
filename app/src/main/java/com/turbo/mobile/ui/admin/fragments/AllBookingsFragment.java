package com.turbo.mobile.ui.admin.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.PlatformEarnings;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentAllBookingsBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.admin.adapters.AllBookingsAdapter;
import com.turbo.mobile.ui.admin.viewmodels.AllBookingsViewModel;
import com.turbo.mobile.ui.driver.BookingDetailActivity;

import java.time.YearMonth;
import java.util.List;

/**
 * Admin fragment displaying platform earnings summary and all bookings filtered by status.
 */
public class AllBookingsFragment extends Fragment {

    private FragmentAllBookingsBinding binding;
    private AllBookingsViewModel viewModel;
    private AllBookingsAdapter adapter;
    private List<YearMonth> months;
    private YearMonth selectedMonth;
    private BookingStatus selectedStatus = null;

    public AllBookingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllBookingsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new AllBookingsViewModel(
                appContainer.bookingRepository,
                appContainer.paymentRepository,
                appContainer.sessionManager);

        adapter = new AllBookingsAdapter(booking -> {
            Intent intent = new Intent(requireContext(), BookingDetailActivity.class);
            intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
            startActivity(intent);
        });
        binding.recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBookings.setAdapter(adapter);

        setUpMonthSpinner();
        setUpStatusSpinner();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEarnings();
        loadBookings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpMonthSpinner() {
        months = viewModel.getPaymentMonths();
        if (months.isEmpty()) {
            return;
        }
        selectedMonth = months.get(0);

        String[] options = new String[months.size()];
        for (int i = 0; i < months.size(); i++) {
            options[i] = months.get(i).toString();
        }
        ScreenSetup.fillSpinner(binding.monthSpinner, options);

        binding.monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = months.get(position);
                loadEarnings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpStatusSpinner() {
        BookingStatus[] statuses = BookingStatus.values();
        String[] options = new String[statuses.length + 1];
        options[0] = getString(R.string.all_bookings_status_all);
        for (int i = 0; i < statuses.length; i++) {
            options[i + 1] = statuses[i].name();
        }
        ScreenSetup.fillSpinner(binding.statusSpinner, options);

        binding.statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedStatus = null;
                } else {
                    selectedStatus = statuses[position - 1];
                }
                loadBookings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadEarnings() {
        if (binding == null || selectedMonth == null) {
            return;
        }
        binding.errorBanner.setVisibility(View.GONE);
        Result<PlatformEarnings> result = viewModel.getPlatformEarnings(selectedMonth);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<PlatformEarnings>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        PlatformEarnings earnings = ((Result.Success<PlatformEarnings>) result).data;
        binding.statPlatformFee.setText(DisplayFormat.money(earnings.getFeesCents()));
        binding.statPayments.setText(DisplayFormat.money(earnings.getPaymentsCents()));
        binding.statCompleted.setText(String.valueOf(earnings.getCompletedCount()));
    }

    private void loadBookings() {
        if (binding == null || viewModel == null) {
            return;
        }
        List<BookingListItem> list = viewModel.getBookings(selectedStatus);
        adapter.setItems(list);
        binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
