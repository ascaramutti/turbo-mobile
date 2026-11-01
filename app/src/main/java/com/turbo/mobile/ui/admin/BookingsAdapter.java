package com.turbo.mobile.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.databinding.ItemBookingBinding;
import com.turbo.mobile.ui.TurboUi;
import com.turbo.mobile.ui.driver.BookingChips;

import java.util.ArrayList;
import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingListItem booking);
    }

    private final List<BookingListItem> bookings;
    private final OnBookingClickListener listener;

    public BookingsAdapter(List<BookingListItem> bookings, OnBookingClickListener listener) {
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
        BookingListItem booking = bookings.get(position);
        Context context = holder.binding.getRoot().getContext();
        // "#1042 · Toyota Corolla"
        holder.binding.txtTitle.setText(context.getString(R.string.bookingTitleFormat,
                booking.getId(), TurboUi.withoutYear(booking.getVehicleTitle())));
        // "Ana L. → Maria L. · Oct 14"
        holder.binding.txtRoute.setText(context.getString(R.string.bookingRouteFormat,
                booking.getDriverName(), booking.getOwnerName(), TurboUi.date(booking.getStart())));
        holder.binding.txtAmount.setText(TurboUi.moneyWithoutCents(booking.getTotalPriceCents()));
        // Same chips as the driver screens: status and payment.
        BookingChips.paintStatus(holder.binding.chipStatus, booking);
        BookingChips.paintPayment(holder.binding.chipPayment, booking);
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
}
