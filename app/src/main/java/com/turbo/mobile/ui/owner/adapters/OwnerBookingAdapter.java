package com.turbo.mobile.ui.owner.adapters;

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
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.databinding.ItemOwnerBookingBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.owner.fragments.BookingRequestsFragment;
import com.turbo.mobile.util.ImageStorage;

import java.util.ArrayList;
import java.util.List;

public class OwnerBookingAdapter extends RecyclerView.Adapter<OwnerBookingAdapter.OwnerBookingViewHolder> {

    private final ImageStorage imageStorage;
    private final BookingRequestsFragment fragment;
    private List<BookingListItem> bookings = new ArrayList<>();

    public OwnerBookingAdapter(ImageStorage imageStorage, BookingRequestsFragment fragment) {
        this.imageStorage = imageStorage;
        this.fragment = fragment;
    }

    public void setItems(List<BookingListItem> bookings) {
        this.bookings = bookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OwnerBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOwnerBookingBinding binding = ItemOwnerBookingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.vehicleThumbnail.setClipToOutline(true);
        return new OwnerBookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerBookingViewHolder holder, int position) {
        BookingListItem booking = bookings.get(position);
        ItemOwnerBookingBinding binding = holder.binding;
        Resources resources = binding.getRoot().getResources();

        if (booking.getCounterpartRatingAvg() == null) {
            binding.driverName.setText(booking.getDriverName());
        } else {
            binding.driverName.setText(resources.getString(R.string.booking_requests_driver_rating, booking.getDriverName(), booking.getCounterpartRatingAvg()));
        }
        binding.vehicleTitle.setText(booking.getVehicleTitle());
        String day = DisplayFormat.date(booking.getStart());
        String timeRange = DisplayFormat.timeRange(booking.getStart(), booking.getEnd());
        String price = DisplayFormat.money(booking.getTotalPriceCents());
        binding.bookingTime.setText(resources.getString(R.string.booking_requests_time, day, timeRange, price));
        binding.vehiclePhoto.setImageBitmap(imageStorage.load(booking.getThumbnailPath(), 400));

        binding.chips.setVisibility(View.VISIBLE);
        binding.chipPayment.setVisibility(View.GONE);
        switch (booking.getStatus()) {
            case PENDING:
                binding.chips.setVisibility(View.GONE);
                break;
            case CONFIRMED:
                paintChip(binding.chipStatus, R.string.booking_requests_chip_confirmed, R.color.turbo_success_bg, R.color.turbo_success);
                binding.chipPayment.setVisibility(View.VISIBLE);
                if (booking.isPaid()) {
                    paintChip(binding.chipPayment, R.string.booking_requests_chip_paid, R.color.turbo_success_bg, R.color.turbo_success);
                } else {
                    paintChip(binding.chipPayment, R.string.booking_requests_chip_unpaid, R.color.turbo_warning_bg, R.color.turbo_warning);
                }
                break;
            case IN_PROGRESS:
                if (booking.isOverdue()) {
                    paintChip(binding.chipStatus, R.string.booking_requests_chip_overdue, R.color.turbo_error_bg, R.color.turbo_error);
                } else {
                    paintChip(binding.chipStatus, R.string.booking_requests_chip_in_progress, R.color.turbo_warning_bg, R.color.turbo_warning);
                }
                break;
            case COMPLETED:
                paintChip(binding.chipStatus, R.string.booking_requests_chip_completed, R.color.turbo_success_bg, R.color.turbo_success);
                break;
            case REJECTED:
                paintChip(binding.chipStatus, R.string.booking_requests_chip_rejected, R.color.turbo_error_bg, R.color.turbo_error);
                break;
            case CANCELLED:
                if (StatusReasons.EXPIRED.equals(booking.getStatusReason())) {
                    paintChip(binding.chipStatus, R.string.booking_requests_chip_expired, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                } else {
                    paintChip(binding.chipStatus, R.string.booking_requests_chip_cancelled, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                }
                break;
        }

        List<OwnerAction> actions = booking.getOwnerActions();
        binding.actions.setVisibility(actions.contains(OwnerAction.CONFIRM) ? View.VISIBLE : View.GONE);
        binding.reject.setOnClickListener(view -> fragment.openRejectDialog(booking));
        binding.confirm.setOnClickListener(view -> fragment.confirm(booking));
        binding.getRoot().setOnClickListener(view -> fragment.openBooking(booking));
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

    static class OwnerBookingViewHolder extends RecyclerView.ViewHolder {

        final ItemOwnerBookingBinding binding;

        OwnerBookingViewHolder(ItemOwnerBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
