package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

/** W-08 My bookings: the driver's upcoming bookings, the trip in progress and the history, in three tabs. */
public class DriverBookingsFragment extends Fragment {

    private AppContainer container;
    private long driverId;
    private int currentTab = R.id.tab_upcoming; // id of the selected tab
    private TextView upcomingTab;
    private TextView inProgressTab;
    private TextView historyTab;
    private View stats;
    private TextView weekHours;
    private TextView thisMonth;
    private TextView totalSpent;
    private TextView completedShifts;
    private LinearLayout bookingList;
    private View emptyText;

    public DriverBookingsFragment() {
        super(R.layout.fragment_driver_bookings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();
        upcomingTab = view.findViewById(R.id.tab_upcoming);
        inProgressTab = view.findViewById(R.id.tab_in_progress);
        historyTab = view.findViewById(R.id.tab_history);
        stats = view.findViewById(R.id.stats);
        weekHours = view.findViewById(R.id.week_hours);
        thisMonth = view.findViewById(R.id.this_month);
        totalSpent = view.findViewById(R.id.total_spent);
        completedShifts = view.findViewById(R.id.completed_shifts);
        bookingList = view.findViewById(R.id.booking_list);
        emptyText = view.findViewById(R.id.bookings_empty);

        upcomingTab.setOnClickListener(v -> selectTab(R.id.tab_upcoming));
        inProgressTab.setOnClickListener(v -> selectTab(R.id.tab_in_progress));
        historyTab.setOnClickListener(v -> selectTab(R.id.tab_history));
    }

    // Reload every time the screen is visible again (e.g. after paying or cancelling in W-09).
    @Override
    public void onResume() {
        super.onResume();
        selectTab(currentTab);
    }

    private void selectTab(int tabId) {
        currentTab = tabId;
        upcomingTab.setSelected(tabId == R.id.tab_upcoming);
        inProgressTab.setSelected(tabId == R.id.tab_in_progress);
        historyTab.setSelected(tabId == R.id.tab_history);
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
        if (tabId == R.id.tab_history || hoursResult instanceof Result.Error || spendingResult instanceof Result.Error) {
            stats.setVisibility(View.GONE);
            return;
        }
        WeeklyHours hours = ((Result.Success<WeeklyHours>) hoursResult).data;
        DriverSpending spending = ((Result.Success<DriverSpending>) spendingResult).data;
        // (int) drops the decimals: shifts are 4 or 8 hours, so the total is a whole number.
        weekHours.setText(getString(R.string.hours_of_limit, (int) hours.getBookedHours(), hours.getLimitHours()));
        thisMonth.setText(TurboUi.money(spending.getMonthCents()));
        totalSpent.setText(TurboUi.money(spending.getTotalCents()));
        completedShifts.setText(String.valueOf(spending.getCompletedCount()));
        stats.setVisibility(View.VISIBLE);
    }

    private void showBookings(List<BookingListItem> bookings, int tabId) {
        bookingList.removeAllViews();
        for (BookingListItem booking : bookings) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, bookingList, false);
            fillCard(card, booking, tabId);
            bookingList.addView(card);
        }
        emptyText.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Vehicle, when, the status chips and a grey line that says what happens next.
    private void fillCard(View card, BookingListItem booking, int tabId) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView statusChip = card.findViewById(R.id.chip_primary);
        TextView paymentChip = card.findViewById(R.id.chip_secondary);
        TextView footer = card.findViewById(R.id.card_footer);

        title.setText(TurboUi.withoutYear(booking.getVehicleTitle()));
        if (tabId == R.id.tab_history) {
            // "Oct 2 · 4 hours"
            String hours = getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
            subtitle.setText(TurboUi.date(booking.getStart()) + " · " + hours);
        } else {
            // "Oct 14 · 08:00–12:00"
            subtitle.setText(TurboUi.date(booking.getStart()) + " · "
                    + TurboUi.timeRange(booking.getStart(), booking.getEnd()));
        }
        BookingChips.paintStatus(statusChip, booking);

        if (BookingChips.isExpired(booking)) {
            footer.setText(R.string.no_owner_response);
        } else if (booking.getStatus() == BookingStatus.PENDING) {
            footer.setText(R.string.waiting_for_owner);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            BookingChips.paintPayment(paymentChip, booking);
            if (booking.isPaid()) {
                footer.setText(R.string.ready_to_start);
            } else {
                footer.setText(getString(R.string.pay_now, TurboUi.money(booking.getTotalPriceCents())));
            }
        } else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            BookingChips.paintPayment(paymentChip, booking);
            footer.setText(getString(R.string.trip_ends_at, TurboUi.time(booking.getEnd())));
        } else if (booking.getStatus() == BookingStatus.COMPLETED) {
            BookingChips.paintPayment(paymentChip, booking);
            footer.setText(TurboUi.money(booking.getTotalPriceCents()));
        } else if (booking.getStatus() == BookingStatus.REJECTED) {
            footer.setText(R.string.request_declined);
        } else { // CANCELLED
            footer.setText(R.string.cancelled_before_payment);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DriverBookingDetailActivity.class);
            intent.putExtra(DriverBookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
            startActivity(intent);
        });
    }
}
