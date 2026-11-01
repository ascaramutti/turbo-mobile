package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.databinding.ActivityBookingDetailBinding;
import com.turbo.mobile.ui.TurboUi;
import com.turbo.mobile.ui.driver.BookingChips;

public class BookingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "EXTRA_BOOKING_ID";

    private ActivityBookingDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);

        long bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        binding.btnViewInspectionPhotos.setOnClickListener(v -> {
            Intent intent = new Intent(this, InspectionPhotosActivity.class);
            intent.putExtra(EXTRA_BOOKING_ID, bookingId);
            startActivity(intent);
        });

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<BookingDetail> result = container.bookingRepository.getBookingDetail(adminId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    showBooking(((Result.Success<BookingDetail>) result).data);
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    private void showBooking(BookingDetail booking) {
        binding.txtBookingNumber.setText(getString(R.string.bookingNumberFormat, booking.getId()));
        binding.txtVehicle.setText(booking.getVehicleTitle());
        binding.txtPeople.setText(getString(R.string.peopleFormat, booking.getDriverName(), booking.getOwnerName()));
        BookingChips.paintStatus(binding.chipStatus, booking);
        // "Oct 10 · 8 hours"
        String hours = getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
        binding.txtWhen.setText(getString(R.string.twoPartsFormat, TurboUi.date(booking.getStart()), hours));

        binding.txtDriverPaid.setText(getString(R.string.driverPaysFormat, TurboUi.money(booking.getTotalPriceCents())));
        BookingChips.paintPayment(binding.chipPayment, booking);
        // The owner's and the platform's parts only exist once the driver paid.
        if (booking.getOwnerPayoutCents() == null) {
            binding.txtSplit.setVisibility(View.GONE);
        } else {
            binding.txtSplit.setText(getString(R.string.paymentSplitFormat,
                    TurboUi.money(booking.getOwnerPayoutCents()), TurboUi.money(booking.getFeeCents())));
        }

        binding.txtService.setText(booking.getServiceType() == ServiceType.TAXI
                ? R.string.service_taxi
                : R.string.service_delivery);
        binding.txtAddress.setText(booking.getPickupAddress());

        // Photos only exist once the trip started.
        if (booking.getPickupPhotoCount() == 0) {
            binding.btnViewInspectionPhotos.setVisibility(View.GONE);
        }
    }
}
