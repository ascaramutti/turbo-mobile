package com.turbo.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.databinding.ItemBookingBinding;

import java.util.ArrayList;
import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingRow booking);
    }

    private final List<BookingRow> bookings;
    private final OnBookingClickListener listener;

    public BookingsAdapter(List<BookingRow> bookings, OnBookingClickListener listener) {
        this.bookings = bookings != null ? bookings : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookingBinding binding = ItemBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingRow booking = bookings.get(position);
        holder.binding.txtTitle.setText(booking.title);
        holder.binding.txtRoute.setText(booking.route);
        holder.binding.txtAmount.setText(booking.amount);
        StatusChip.apply(holder.binding.chipStatus, booking.status);
        StatusChip.apply(holder.binding.chipPayment, booking.payment);
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final ItemBookingBinding binding;

        BookingViewHolder(ItemBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /** Simple display model for a booking row. Replaced by the real model when we connect the data. */
    public static class BookingRow {
        public final String title;
        public final String route;
        public final String amount;
        public final String status;
        public final String payment;

        public BookingRow(String title, String route, String amount, String status, String payment) {
            this.title = title;
            this.route = route;
            this.amount = amount;
            this.status = status;
            this.payment = payment;
        }
    }
}
