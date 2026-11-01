package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.databinding.ActivityAllBookingsBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

public class AllBookingsActivity extends AppCompatActivity {

    private ActivityAllBookingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);
        TurboUi.setUpAdminBar(this);

        binding.recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        // TODO(filters): shows every booking; the month and status filters and the stats above aren't connected yet.
        AppContainer container = ((TurboApp) getApplication()).getContainer();
        container.executor.execute(() -> {
            List<BookingListItem> bookings = container.bookingRepository.getAllBookings(null);
            runOnUiThread(() -> binding.recyclerBookings.setAdapter(
                    new BookingsAdapter(bookings, booking -> openBooking(booking))));
        });
    }

    private void openBooking(BookingListItem booking) {
        Intent intent = new Intent(this, BookingDetailActivity.class);
        intent.putExtra(BookingDetailActivity.EXTRA_BOOKING_ID, booking.getId());
        startActivity(intent);
    }
}
