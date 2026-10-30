package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.ui.ReviewActivity;
import com.turbo.mobile.ui.TurboUi;

import java.util.Locale;

/** W-09 Booking detail (driver): the booking's status and its next step (pay, start, end, rate...). */
public class DriverBookingDetailActivity extends AppCompatActivity {

    // Name of the Intent extra with the booking's id (sent by DriverBookingsFragment).
    public static final String EXTRA_BOOKING_ID = "booking_id";

    private AppContainer container;
    private long driverId;
    private long bookingId;
    private BookingDetail booking;

    private TextView vehicleSummary;
    private TextView infoBanner;
    private TextView warningBanner;
    private TextView successBanner;
    private TextView errorBanner;
    private LinearLayout cardList;
    private Button mainButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_driver_booking_detail);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();
        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);

        TextView title = findViewById(R.id.booking_title);
        title.setText(getString(R.string.booking_number, bookingId));
        vehicleSummary = findViewById(R.id.vehicle_summary);
        infoBanner = findViewById(R.id.banner_info);
        warningBanner = findViewById(R.id.banner_warning);
        successBanner = findViewById(R.id.banner_success);
        errorBanner = findViewById(R.id.banner_error);
        cardList = findViewById(R.id.card_list);
        mainButton = findViewById(R.id.main_action);
        cancelButton = findViewById(R.id.cancel_booking);
        cancelButton.setOnClickListener(v -> confirmCancel());
    }

    // Load in onResume: after paying, taking the trip photos or rating, this screen shows the new status.
    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        container.executor.execute(() -> {
            Result<BookingDetail> result = container.bookingRepository.getBookingDetail(driverId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    showBooking(((Result.Success<BookingDetail>) result).data);
                } else {
                    TurboUi.showError(this, result);
                    finish();
                }
            });
        });
    }

    // Clears the screen and shows the version of the booking's status.
    private void showBooking(BookingDetail detail) {
        booking = detail;
        vehicleSummary.setText(booking.getVehicleTitle());
        infoBanner.setVisibility(View.GONE);
        warningBanner.setVisibility(View.GONE);
        successBanner.setVisibility(View.GONE);
        errorBanner.setVisibility(View.GONE);
        cardList.removeAllViews();
        mainButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);

        if (booking.getStatus() == BookingStatus.PENDING) {
            showPending();
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            showConfirmed();
        } else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            showInProgress();
        } else if (booking.getStatus() == BookingStatus.COMPLETED) {
            showCompleted();
        } else {
            showClosed(); // REJECTED, CANCELLED or expired
        }
    }

    // Pending: waiting for the owner. The driver can still cancel.
    private void showPending() {
        showBanner(warningBanner, getString(R.string.banner_waiting_owner));

        // "Owner / Maria L. · ★ 4.8 / Toyota Corolla"
        String owner = booking.getOwnerName();
        if (booking.getCounterpartRatingAvg() != null) { // null when the owner has no reviews yet
            owner = owner + " · ★ " + String.format(Locale.US, "%.1f", booking.getCounterpartRatingAvg());
        }
        addCard(getString(R.string.owner), owner, TurboUi.withoutYear(booking.getVehicleTitle()));

        // "Shift / Wed, Oct 14 · 08:00–12:00 · 4 hours / Delivery / 8888 University Dr, Burnaby"
        View shift = addCard(getString(R.string.card_shift), whenWithHours(), booking.getPickupAddress());
        TurboUi.paintChip(shift.findViewById(R.id.chip_primary), serviceName(),
                R.color.turbo_primary_container, R.color.turbo_primary);

        // "Amount / CAD 50.00 / Payment unavailable / Waiting for owner"
        View amount = addCard(getString(R.string.card_amount), TurboUi.money(booking.getTotalPriceCents()),
                getString(R.string.waiting_for_owner));
        TurboUi.paintChip(amount.findViewById(R.id.chip_primary), getString(R.string.chip_payment_unavailable),
                R.color.turbo_neutral_bg, R.color.turbo_text_secondary);

        cancelButton.setVisibility(View.VISIBLE);
    }

    // Confirmed: first pay ("Pay CAD 50.00" or cancel), then start the trip.
    private void showConfirmed() {
        String when = TurboUi.dayAndDate(booking.getStart()) + " · "
                + TurboUi.timeRange(booking.getStart(), booking.getEnd());
        String price = TurboUi.money(booking.getTotalPriceCents());
        if (booking.isPaid()) {
            showBanner(successBanner, getString(R.string.banner_booking_ready));
            View card = addCard(getString(R.string.card_booking), when, paymentSplit());
            paintChips(card);
            showMainButton(getString(R.string.start_trip), v -> startTrip());
        } else {
            showBanner(warningBanner, getString(R.string.banner_payment_required));
            View card = addCard(getString(R.string.card_booking), when, serviceName() + " · " + price);
            paintChips(card);
            showMainButton(getString(R.string.pay_amount, price), v -> openPayment());
            cancelButton.setVisibility(View.VISIBLE);
        }
    }

    // In progress: the trip ends with the return photos.
    private void showInProgress() {
        showBanner(successBanner, getString(R.string.banner_trip_in_progress));
        String hours = getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
        View card = addCard(getString(R.string.card_current_trip),
                TurboUi.timeRange(booking.getStart(), booking.getEnd()) + " · " + hours,
                getString(R.string.return_photos_required));
        paintChips(card);
        TurboUi.showCardPhoto(this, card, booking.getThumbnailPath());
        showMainButton(getString(R.string.end_trip), v -> openTripPhotos(true));
    }

    // Completed: the summary, the inspection photos and "Rate your experience" until the driver rates the owner.
    private void showCompleted() {
        showBanner(successBanner, getString(R.string.banner_trip_completed));
        String hours = getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
        View summary = addCard(getString(R.string.card_trip_summary),
                TurboUi.date(booking.getStart()) + " · " + hours + " · " + serviceName(),
                TurboUi.money(booking.getTotalPriceCents()));
        paintChips(summary);

        // "Inspection photos / Pickup 5 · Return 4"
        View photos = addCard(getString(R.string.card_inspection_photos),
                getString(R.string.photo_counts, booking.getPickupPhotoCount(), booking.getReturnPhotoCount()),
                getString(R.string.photos_saved));
        TurboUi.showCardPhoto(this, photos, booking.getThumbnailPath());

        if (!booking.isReviewedByDriver()) {
            showMainButton(getString(R.string.rate_title), v -> openReview());
        }
    }

    // Rejected, expired or cancelled: the booking is closed. The driver can look for another vehicle.
    private void showClosed() {
        String footer = getString(R.string.request_closed);
        if (BookingChips.isExpired(booking)) {
            showBanner(infoBanner, getString(R.string.banner_expired));
        } else if (booking.getStatus() == BookingStatus.REJECTED) {
            showBanner(errorBanner, getString(R.string.banner_rejected));
        } else {
            showBanner(infoBanner, getString(R.string.banner_cancelled));
            footer = getString(R.string.booking_history_preserved);
        }
        View card = addCard(getString(R.string.card_final_status), getString(R.string.no_payment_made), footer);
        paintChips(card);
        showMainButton(getString(R.string.find_another_vehicle), v -> findAnotherVehicle());
    }

    private void showBanner(TextView banner, String text) {
        banner.setText(text);
        banner.setVisibility(View.VISIBLE);
    }

    // Adds an item_info_card with a title, a grey line under it and a grey line at the bottom.
    private View addCard(String title, String subtitle, String footer) {
        View card = getLayoutInflater().inflate(R.layout.item_info_card, cardList, false);
        TextView titleView = card.findViewById(R.id.card_title);
        TextView subtitleView = card.findViewById(R.id.card_subtitle);
        TextView footerView = card.findViewById(R.id.card_footer);
        titleView.setText(title);
        subtitleView.setText(subtitle);
        footerView.setText(footer);
        cardList.addView(card);
        return card;
    }

    // The status chip and the payment chip, e.g. "Confirmed" + "Payment pending".
    private void paintChips(View card) {
        BookingChips.paintStatus(card.findViewById(R.id.chip_primary), booking);
        BookingChips.paintPayment(card.findViewById(R.id.chip_secondary), booking);
    }

    private void showMainButton(String text, View.OnClickListener action) {
        mainButton.setText(text);
        mainButton.setOnClickListener(action);
        mainButton.setVisibility(View.VISIBLE);
    }

    // "Wed, Oct 14 · 08:00–12:00 · 4 hours"
    private String whenWithHours() {
        String hours = getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
        return TurboUi.dayAndDate(booking.getStart()) + " · "
                + TurboUi.timeRange(booking.getStart(), booking.getEnd()) + " · " + hours;
    }

    // "Taxi" or "Delivery"
    private String serviceName() {
        if (booking.getServiceType() == ServiceType.TAXI) {
            return getString(R.string.service_taxi);
        }
        return getString(R.string.service_delivery);
    }

    // "Payment split: owner CAD 40.00 · platform CAD 10.00". The owner receives 80%, the platform the rest.
    private String paymentSplit() {
        int ownerShare = booking.getTotalPriceCents() * 80 / 100;
        int platformShare = booking.getTotalPriceCents() - ownerShare;
        return getString(R.string.payment_split, TurboUi.money(ownerShare), TurboUi.money(platformShare));
    }

    private void confirmCancel() {
        TurboUi.showConfirmDialog(this, getString(R.string.cancel_booking_title),
                getString(R.string.cancel_booking_body), getString(R.string.keep_booking),
                getString(R.string.cancel_booking), () -> cancelBooking());
    }

    private void cancelBooking() {
        container.executor.execute(() -> {
            Result<Void> result = container.bookingRepository.cancelBooking(driverId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.booking_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    TurboUi.showError(this, result);
                }
                load();
            });
        });
    }

    private void openPayment() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_BOOKING_ID, bookingId);
        startActivity(intent);
    }

    // Before the pickup photos, ask the repository if the trip can start now (paid, inside the start window).
    private void startTrip() {
        container.executor.execute(() -> {
            Result<Void> result = container.bookingRepository.canStart(driverId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    openTripPhotos(false);
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    // W-11 with the pickup photos (isReturn false) or the return photos (isReturn true).
    private void openTripPhotos(boolean isReturn) {
        Intent intent = new Intent(this, TripPhotosActivity.class);
        intent.putExtra(TripPhotosActivity.EXTRA_BOOKING_ID, bookingId);
        intent.putExtra(TripPhotosActivity.EXTRA_IS_RETURN, isReturn);
        startActivity(intent);
    }

    // W-12: the driver rates the car owner.
    private void openReview() {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra(ReviewActivity.EXTRA_BOOKING_ID, bookingId);
        intent.putExtra(ReviewActivity.EXTRA_QUESTION, getString(R.string.rate_question_owner, booking.getOwnerName()));
        startActivity(intent);
    }

    // Back to the driver's home, which opens on Search. CLEAR_TOP closes the screens on top of it (this one too).
    private void findAnotherVehicle() {
        Intent intent = new Intent(this, DriverActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
