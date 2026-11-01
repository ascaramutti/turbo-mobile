package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.TurboUi;

/** W-10 Payment: a simulated card payment of a confirmed booking. Nothing is really charged. */
public class PaymentActivity extends AppCompatActivity {

    // Name of the Intent extra with the booking's id (sent by DriverBookingDetailActivity).
    public static final String EXTRA_BOOKING_ID = "booking_id";

    // Stripe's test card that is always declined. W-10 "Invalid card" shows it as invalid.
    private static final String DECLINED_TEST_CARD = "4000000000000002";

    private AppContainer container;
    private long driverId;
    private long bookingId;
    private BookingDetail booking;

    private TextView title;
    private TextView subtitle;
    private View formSection;
    private View infoBanner;
    private View errorBanner;
    private EditText cardInput;
    private EditText expiryInput;
    private EditText cvvInput;
    private View cardError;
    private TextView scheduleTime;
    private TextView scheduleAmount;
    private View statusSection;
    private View statusProgress;
    private ImageView statusIcon;
    private TextView statusTitle;
    private TextView statusMessage;
    private Button mainButton;
    private Button secondButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_payment);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();
        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);

        title = findViewById(R.id.payment_title);
        subtitle = findViewById(R.id.payment_subtitle);
        formSection = findViewById(R.id.form_section);
        infoBanner = findViewById(R.id.banner_info);
        errorBanner = findViewById(R.id.banner_error);
        cardInput = findViewById(R.id.input_card);
        expiryInput = findViewById(R.id.input_expiry);
        cvvInput = findViewById(R.id.input_cvv);
        cardError = findViewById(R.id.card_error);
        scheduleTime = findViewById(R.id.schedule_time);
        scheduleAmount = findViewById(R.id.schedule_amount);
        statusSection = findViewById(R.id.status_section);
        statusProgress = findViewById(R.id.status_progress);
        statusIcon = findViewById(R.id.status_icon);
        statusTitle = findViewById(R.id.status_title);
        statusMessage = findViewById(R.id.status_message);
        mainButton = findViewById(R.id.main_action);
        secondButton = findViewById(R.id.second_action);

        mainButton.setEnabled(false); // until the booking is loaded
        loadBooking();
    }

    private void loadBooking() {
        container.executor.execute(() -> {
            Result<BookingDetail> result = container.bookingRepository.getBookingDetail(driverId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    booking = ((Result.Success<BookingDetail>) result).data;
                    showForm();
                } else {
                    TurboUi.showError(this, result);
                    finish();
                }
            });
        });
    }

    // The card form. Also used by "Retry payment".
    private void showForm() {
        String price = TurboUi.money(booking.getTotalPriceCents());
        title.setText(R.string.payment);
        subtitle.setText(getString(R.string.payment_subtitle, bookingId, TurboUi.withoutYear(booking.getVehicleTitle())));
        subtitle.setVisibility(View.VISIBLE);
        scheduleTime.setText(TurboUi.dayAndDate(booking.getStart()) + " · "
                + TurboUi.timeRange(booking.getStart(), booking.getEnd()));
        scheduleAmount.setText(getString(R.string.amount_value, price));
        formSection.setVisibility(View.VISIBLE);
        statusSection.setVisibility(View.GONE);

        mainButton.setText(getString(R.string.pay_amount, price));
        mainButton.setEnabled(true);
        mainButton.setOnClickListener(v -> pay());
        secondButton.setText(R.string.cancel);
        secondButton.setVisibility(View.VISIBLE);
        secondButton.setOnClickListener(v -> finish());
    }

    private void pay() {
        String card = cardInput.getText().toString().replace(" ", ""); // "4242 4242..." -> "4242424242424242"
        String expiry = expiryInput.getText().toString().trim();
        String cvv = cvvInput.getText().toString().trim();

        // Simple checks for the simulated payment: 16 digits, a date like 10/29 and 3 digits.
        boolean cardOk = card.length() == 16 && !card.equals(DECLINED_TEST_CARD);
        boolean expiryOk = expiry.length() == 5 && expiry.charAt(2) == '/';
        boolean cvvOk = cvv.length() == 3;
        if (!cardOk || !expiryOk || !cvvOk) {
            infoBanner.setVisibility(View.GONE);
            errorBanner.setVisibility(View.VISIBLE);
            cardError.setVisibility(cardOk ? View.GONE : View.VISIBLE); // "Card details are invalid"
            return;
        }
        infoBanner.setVisibility(View.VISIBLE);
        errorBanner.setVisibility(View.GONE);
        cardError.setVisibility(View.GONE);

        showProcessing();
        String last4 = card.substring(12); // only the last 4 digits are saved
        // A real payment takes a moment, so the simulated one waits 1.5 seconds before paying.
        mainButton.postDelayed(() -> sendPayment(last4), 1500);
    }

    private void sendPayment(String last4) {
        container.executor.execute(() -> {
            Result<Void> result = container.paymentRepository.registerPayment(driverId, bookingId, last4);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    showComplete();
                } else {
                    TurboUi.showError(this, result); // e.g. the payment deadline passed
                    showFailed();
                }
            });
        });
    }

    private void showProcessing() {
        subtitle.setText(getString(R.string.payment_subtitle, bookingId, TurboUi.money(booking.getTotalPriceCents())));
        showStatus(0, getString(R.string.processing_payment), getString(R.string.processing_body));
        mainButton.setText(R.string.processing);
        mainButton.setEnabled(false);
        secondButton.setVisibility(View.GONE);
    }

    private void showComplete() {
        int total = booking.getTotalPriceCents();
        int ownerShare = total * 80 / 100; // the owner receives 80%, the platform the rest
        title.setText(R.string.payment_complete);
        subtitle.setVisibility(View.GONE);
        showStatus(R.drawable.ic_circle_check, getString(R.string.amount_paid, TurboUi.money(total)),
                getString(R.string.payment_complete_body, bookingId, TurboUi.money(ownerShare),
                        TurboUi.money(total - ownerShare)));
        mainButton.setText(R.string.view_booking);
        mainButton.setEnabled(true);
        mainButton.setOnClickListener(v -> finish()); // the booking detail reloads in onResume
    }

    private void showFailed() {
        title.setText(R.string.payment_failed);
        subtitle.setVisibility(View.GONE);
        showStatus(R.drawable.ic_circle_x, getString(R.string.payment_failed_title),
                getString(R.string.payment_failed_body));
        mainButton.setText(R.string.retry_payment);
        mainButton.setEnabled(true);
        mainButton.setOnClickListener(v -> showForm());
        secondButton.setText(R.string.back_to_booking);
        secondButton.setVisibility(View.VISIBLE);
        secondButton.setOnClickListener(v -> finish());
    }

    // Hides the form and shows the circle with a title and a message. icon 0 = the spinning circle.
    private void showStatus(int icon, String titleText, String message) {
        formSection.setVisibility(View.GONE);
        statusSection.setVisibility(View.VISIBLE);
        if (icon == 0) {
            statusProgress.setVisibility(View.VISIBLE);
            statusIcon.setVisibility(View.GONE);
        } else {
            statusProgress.setVisibility(View.GONE);
            statusIcon.setImageResource(icon);
            statusIcon.setVisibility(View.VISIBLE);
        }
        statusTitle.setText(titleText);
        statusMessage.setText(message);
    }
}
