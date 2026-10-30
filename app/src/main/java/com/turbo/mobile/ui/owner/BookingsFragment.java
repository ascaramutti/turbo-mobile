package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.ui.ReviewActivity;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;
import java.util.Locale;

/** W-18 Bookings: the owner's booking requests, upcoming bookings and history, in three tabs. */
public class BookingsFragment extends Fragment {

    private AppContainer container;
    private long ownerId;
    private int currentTab = R.id.tab_requests; // id of the selected tab
    private TextView requestsTab;
    private TextView upcomingTab;
    private TextView historyTab;
    private LinearLayout bookingList;
    private View emptyText;

    public BookingsFragment() {
        super(R.layout.fragment_bookings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        requestsTab = view.findViewById(R.id.tab_requests);
        upcomingTab = view.findViewById(R.id.tab_upcoming);
        historyTab = view.findViewById(R.id.tab_history);
        bookingList = view.findViewById(R.id.booking_list);
        emptyText = view.findViewById(R.id.bookings_empty);

        requestsTab.setOnClickListener(v -> selectTab(R.id.tab_requests));
        upcomingTab.setOnClickListener(v -> selectTab(R.id.tab_upcoming));
        historyTab.setOnClickListener(v -> selectTab(R.id.tab_history));
    }

    // Reload every time the screen is visible again (e.g. after reviewing a driver in W-12).
    @Override
    public void onResume() {
        super.onResume();
        selectTab(currentTab);
    }

    private void selectTab(int tabId) {
        currentTab = tabId;
        requestsTab.setSelected(tabId == R.id.tab_requests);
        upcomingTab.setSelected(tabId == R.id.tab_upcoming);
        historyTab.setSelected(tabId == R.id.tab_history);
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
        bookingList.removeAllViews();
        for (BookingListItem booking : bookings) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, bookingList, false);
            if (tabId == R.id.tab_requests) {
                fillRequestCard(card, booking);
            } else if (tabId == R.id.tab_upcoming) {
                fillUpcomingCard(card, booking);
            } else {
                fillHistoryCard(card, booking);
            }
            bookingList.addView(card);
        }
        emptyText.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Requests: "Ana L. · ★ 4.9", the service and the price, and the "Reject · Confirm" links.
    private void fillRequestCard(View card, BookingListItem booking) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView serviceChip = card.findViewById(R.id.chip_primary);
        TextView priceChip = card.findViewById(R.id.chip_secondary);

        String name = booking.getDriverName();
        if (booking.getCounterpartRatingAvg() != null) { // null when the driver has no reviews yet
            name = name + " · ★ " + String.format(Locale.US, "%.1f", booking.getCounterpartRatingAvg());
        }
        title.setText(name);
        subtitle.setText(shift(booking));

        String service = getString(R.string.service_delivery);
        if (booking.getServiceType() == ServiceType.TAXI) {
            service = getString(R.string.service_taxi);
        }
        TurboUi.paintChip(serviceChip, service, R.color.turbo_primary_container, R.color.turbo_primary);
        TurboUi.paintChip(priceChip, TurboUi.money(booking.getTotalPriceCents()),
                R.color.turbo_primary_container, R.color.turbo_primary);

        card.findViewById(R.id.card_footer).setVisibility(View.GONE);
        card.findViewById(R.id.card_actions).setVisibility(View.VISIBLE);
        card.findViewById(R.id.action_reject).setOnClickListener(v -> reject(booking));
        card.findViewById(R.id.action_confirm).setOnClickListener(v -> confirm(booking));
    }

    // Upcoming: "Confirmed" or "In progress", "Paid" or "Payment pending", and the price.
    private void fillUpcomingCard(View card, BookingListItem booking) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView statusChip = card.findViewById(R.id.chip_primary);
        TextView paymentChip = card.findViewById(R.id.chip_secondary);
        TextView footer = card.findViewById(R.id.card_footer);

        title.setText(booking.getDriverName());
        subtitle.setText(shift(booking));

        String status = getString(R.string.chip_confirmed);
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            status = getString(R.string.chip_in_progress);
        }
        TurboUi.paintChip(statusChip, status, R.color.turbo_success_bg, R.color.turbo_success);

        String price = TurboUi.money(booking.getTotalPriceCents());
        if (booking.isPaid()) {
            TurboUi.paintChip(paymentChip, getString(R.string.chip_paid), R.color.turbo_success_bg, R.color.turbo_success);
            int ownerShare = booking.getTotalPriceCents() * 80 / 100; // the owner receives 80%
            footer.setText(getString(R.string.price_with_share, price, TurboUi.money(ownerShare)));
        } else {
            TurboUi.paintChip(paymentChip, getString(R.string.chip_payment_pending),
                    R.color.turbo_warning_bg, R.color.turbo_warning);
            footer.setText(price);
        }
    }

    // History: "Completed" + "Paid"/"Not paid" and "Review driver", or "Cancelled"/"Rejected".
    private void fillHistoryCard(View card, BookingListItem booking) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView statusChip = card.findViewById(R.id.chip_primary);
        TextView paymentChip = card.findViewById(R.id.chip_secondary);
        TextView footer = card.findViewById(R.id.card_footer);

        title.setText(booking.getDriverName());
        String when = TurboUi.withoutYear(booking.getVehicleTitle()) + " · " + TurboUi.date(booking.getStart())
                + " · " + booking.getHours() + " h";
        subtitle.setText(when);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            TurboUi.paintChip(statusChip, getString(R.string.chip_completed),
                    R.color.turbo_success_bg, R.color.turbo_success);
            String payment = getString(R.string.chip_not_paid);
            if (booking.isPaid()) {
                payment = getString(R.string.chip_paid);
            }
            TurboUi.paintChip(paymentChip, payment, R.color.turbo_success_bg, R.color.turbo_success);
            // The repository adds REVIEW to the owner's actions until the owner reviews the driver.
            if (booking.getOwnerActions().contains(OwnerAction.REVIEW)) {
                footer.setText(R.string.review_driver);
                footer.setOnClickListener(v -> openReview(booking));
            } else {
                footer.setText(R.string.driver_reviewed);
            }
        } else {
            String status = getString(R.string.chip_rejected);
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                status = getString(R.string.chip_cancelled);
            }
            TurboUi.paintChip(statusChip, status, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
            footer.setText(R.string.history_preserved);
        }
    }

    // "Toyota Corolla · Oct 14 · 08:00–12:00"
    private String shift(BookingListItem booking) {
        return TurboUi.withoutYear(booking.getVehicleTitle()) + " · " + TurboUi.date(booking.getStart())
                + " · " + TurboUi.time(booking.getStart()) + "–" + TurboUi.time(booking.getEnd());
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
