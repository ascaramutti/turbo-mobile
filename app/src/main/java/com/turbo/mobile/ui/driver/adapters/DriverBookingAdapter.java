package com.turbo.mobile.ui.driver.adapters;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.databinding.ItemDriverBookingBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.driver.fragments.MyBookingsFragment;

import java.util.ArrayList;
import java.util.List;

public class DriverBookingAdapter extends RecyclerView.Adapter<DriverBookingAdapter.DriverBookingViewHolder> {

    private final MyBookingsFragment fragment;
    private List<BookingListItem> bookings = new ArrayList<>();

    public DriverBookingAdapter(MyBookingsFragment fragment) {
        this.fragment = fragment;
    }

    public void setItems(List<BookingListItem> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DriverBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDriverBookingBinding binding = ItemDriverBookingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DriverBookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverBookingViewHolder holder, int position) {
        BookingListItem booking = bookings.get(position);
        ItemDriverBookingBinding binding = holder.binding;
        Resources resources = binding.getRoot().getResources();

        binding.vehicleTitle.setText(booking.getVehicleTitle());
        String day = DisplayFormat.date(booking.getStart());
        String timeRange = DisplayFormat.timeRange(booking.getStart(), booking.getEnd());
        String hours = resources.getQuantityString(R.plurals.my_bookings_hours, booking.getHours(), booking.getHours());
        binding.bookingTime.setText(resources.getString(R.string.my_bookings_time, day, timeRange));
        binding.chipPayment.setVisibility(View.VISIBLE);
        if (booking.isPaid()) {
            paintChip(binding.chipPayment, R.string.my_bookings_chip_paid, R.color.turbo_success_bg, R.color.turbo_success);
        } else {
            paintChip(binding.chipPayment, R.string.my_bookings_chip_payment_pending, R.color.turbo_warning_bg, R.color.turbo_warning);
        }

        switch (booking.getStatus()) {
            case PENDING:
                paintChip(binding.chipStatus, R.string.my_bookings_chip_pending, R.color.turbo_warning_bg, R.color.turbo_warning);
                binding.chipPayment.setVisibility(View.GONE);
                binding.bookingNote.setText(R.string.my_bookings_waiting_owner);
                break;
            case CONFIRMED:
                paintChip(binding.chipStatus, R.string.my_bookings_chip_confirmed, R.color.turbo_success_bg, R.color.turbo_success);
                if (booking.isPaid()) {
                    binding.bookingNote.setText(R.string.my_bookings_ready);
                } else {
                    binding.bookingNote.setText(resources.getString(R.string.my_bookings_pay_now, DisplayFormat.money(booking.getTotalPriceCents())));
                }
                break;
            case IN_PROGRESS:
                paintChip(binding.chipStatus, R.string.my_bookings_chip_in_progress, R.color.turbo_warning_bg, R.color.turbo_warning);
                if (booking.isOverdue()) {
                    binding.bookingNote.setText(resources.getString(R.string.my_bookings_overdue, DisplayFormat.time(booking.getEnd())));
                } else {
                    binding.bookingNote.setText(resources.getString(R.string.my_bookings_trip_ends, DisplayFormat.time(booking.getEnd())));
                }
                break;
            case COMPLETED:
                binding.bookingTime.setText(resources.getString(R.string.my_bookings_time, day, hours));
                paintChip(binding.chipStatus, R.string.my_bookings_chip_completed, R.color.turbo_success_bg, R.color.turbo_success);
                binding.bookingNote.setText(DisplayFormat.money(booking.getTotalPriceCents()));
                break;
            case REJECTED:
                binding.bookingTime.setText(resources.getString(R.string.my_bookings_time, day, hours));
                paintChip(binding.chipStatus, R.string.my_bookings_chip_rejected, R.color.turbo_error_bg, R.color.turbo_error);
                binding.chipPayment.setVisibility(View.GONE);
                binding.bookingNote.setText(R.string.my_bookings_declined);
                break;
            case CANCELLED:
                binding.bookingTime.setText(resources.getString(R.string.my_bookings_time, day, hours));
                binding.chipPayment.setVisibility(View.GONE);
                if (StatusReasons.EXPIRED.equals(booking.getStatusReason())) {
                    paintChip(binding.chipStatus, R.string.my_bookings_chip_expired, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                    binding.bookingNote.setText(R.string.my_bookings_no_response);
                } else {
                    paintChip(binding.chipStatus, R.string.my_bookings_chip_cancelled, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                    binding.bookingNote.setText(booking.getStatusReason() == null ? resources.getString(R.string.my_bookings_cancelled_before_payment) : booking.getStatusReason());
                }
                break;
        }
        binding.getRoot().setOnClickListener(v -> fragment.openBooking(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private void paintChip(TextView chip, int text, int backgroundColor, int textColor) {
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(chip.getContext(), backgroundColor));
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), textColor));
    }

    static class DriverBookingViewHolder extends RecyclerView.ViewHolder {

        final ItemDriverBookingBinding binding;

        DriverBookingViewHolder(ItemDriverBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
