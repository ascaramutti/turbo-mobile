package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentBookingsBinding;
import com.turbo.mobile.ui.ReviewActivity;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

/** W-18 Bookings: the owner's booking requests, upcoming bookings and history, in three tabs. */
public class BookingsFragment extends Fragment {

    private FragmentBookingsBinding binding;
    private AppContainer container;
    private long ownerId;
    private int currentTab = R.id.tab_requests; // id of the selected tab
    private OwnerBookingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBookingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();

        adapter = new OwnerBookingsAdapter(new OwnerBookingsAdapter.OnBookingActionListener() {
            @Override
            public void onReject(BookingListItem booking) {
                reject(booking);
            }

            @Override
            public void onConfirm(BookingListItem booking) {
                confirm(booking);
            }

            @Override
            public void onReview(BookingListItem booking) {
                openReview(booking);
            }
        });

        binding.recyclerOwnerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerOwnerBookings.setAdapter(adapter);

        binding.tabRequests.setOnClickListener(v -> selectTab(R.id.tab_requests));
        binding.tabUpcoming.setOnClickListener(v -> selectTab(R.id.tab_upcoming));
        binding.tabHistory.setOnClickListener(v -> selectTab(R.id.tab_history));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Reload every time the screen is visible again (e.g. after reviewing a driver in W-12).
    @Override
    public void onResume() {
        super.onResume();
        selectTab(currentTab);
    }

    private void selectTab(int tabId) {
        if (binding == null) return;
        currentTab = tabId;
        binding.tabRequests.setSelected(tabId == R.id.tab_requests);
        binding.tabUpcoming.setSelected(tabId == R.id.tab_upcoming);
        binding.tabHistory.setSelected(tabId == R.id.tab_history);
        loadBookings();
    }

    private void loadBookings() {
        int tabId = currentTab;
        container.executor.execute(() -> {
            List<BookingListItem> bookings;
            if (tabId == R.id.tab_requests) {
                bookings = container.bookingRepository.getPendingByOwner(ownerId);
            } else if (tabId == R.id.tab_upcoming) {
                bookings = container.bookingRepository.getOwnerBookings(ownerId, OwnerTab.UPCOMING);
            } else {
                bookings = container.bookingRepository.getOwnerBookings(ownerId, OwnerTab.HISTORY);
            }
            TurboUi.runOnMainThread(this, () -> showBookings(bookings, tabId));
        });
    }

    private void showBookings(List<BookingListItem> bookings, int tabId) {
        if (binding == null) return;
        adapter.setItems(bookings, tabId);
        binding.bookingsEmpty.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirm(BookingListItem booking) {
        container.executor.execute(() -> {
            Result<ConfirmResult> result = container.bookingRepository.confirm(ownerId, booking.getId());
            TurboUi.runOnMainThread(this, () -> showResultAndReload(result, R.string.booking_confirmed));
        });
    }

    private void reject(BookingListItem booking) {
        String reason = getString(R.string.reject_reason); // read here: getString needs the screen
        container.executor.execute(() -> {
            Result<Void> result = container.bookingRepository.reject(ownerId, booking.getId(), reason);
            TurboUi.runOnMainThread(this, () -> showResultAndReload(result, R.string.booking_rejected));
        });
    }

    private void showResultAndReload(Result<?> result, int successMessage) {
        if (result instanceof Result.Success) {
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
        } else {
            TurboUi.showError(requireContext(), result);
        }
        loadBookings();
    }

    private void openReview(BookingListItem booking) {
        Intent intent = new Intent(requireContext(), ReviewActivity.class);
        intent.putExtra(ReviewActivity.EXTRA_BOOKING_ID, booking.getId());
        intent.putExtra(ReviewActivity.EXTRA_QUESTION, getString(R.string.rate_question_driver, booking.getDriverName()));
        startActivity(intent);
    }
}
