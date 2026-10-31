package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.databinding.ActivityAllBookingsBinding;

import java.util.ArrayList;
import java.util.List;

public class AllBookingsActivity extends AppCompatActivity {

    private ActivityAllBookingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BookingsAdapter adapter = new BookingsAdapter(sampleBookings(),
                booking -> startActivity(new Intent(this, BookingDetailActivity.class)));
        binding.recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBookings.setAdapter(adapter);
    }

    /** Placeholder list matching the "All bookings" screen of the PDF. Replaced by real data later. */
    private List<BookingsAdapter.BookingRow> sampleBookings() {
        List<BookingsAdapter.BookingRow> bookings = new ArrayList<>();
        bookings.add(new BookingsAdapter.BookingRow(
                "#1042 · Toyota Corolla", "Ana L. → Maria L. · Oct 14", "CAD 50",
                "Confirmed", "Payment pending"));
        bookings.add(new BookingsAdapter.BookingRow(
                "#1039 · Honda Fit", "Kenji S. → Maria L. · Oct 10", "CAD 72",
                "Completed", "Paid"));
        bookings.add(new BookingsAdapter.BookingRow(
                "#1037 · Mazda CX-5", "Raj P. → Tom W. · Oct 9", "CAD 56",
                "Cancelled", "Not paid"));
        return bookings;
    }
}
