package com.turbo.mobile.ui.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.databinding.ItemAdminBookingBinding;
import com.turbo.mobile.ui.DisplayFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter displaying all bookings in the Admin bookings list.
 */
public class AllBookingsAdapter extends RecyclerView.Adapter<AllBookingsAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingListItem booking);
    }

    private final OnBookingClickListener listener;
    private List<BookingListItem> items = new ArrayList<>();

    public AllBookingsAdapter(OnBookingClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BookingListItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminBookingBinding binding = ItemAdminBookingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingListItem item = items.get(position);
        Context context = holder.binding.getRoot().getContext();

        holder.binding.bookingTitle.setText(
                context.getString(R.string.all_bookings_item_title, item.getId(), item.getVehicleTitle()));
        holder.binding.bookingPartiesDate.setText(
                context.getString(R.string.all_bookings_parties_date,
                        item.getDriverName(), item.getOwnerName(), DisplayFormat.date(item.getStart())));

        String formattedPrice = DisplayFormat.money(item.getTotalPriceCents());
        if (item.isPaid()) {
            holder.binding.bookingPaymentPrice.setText(context.getString(R.string.all_bookings_paid, formattedPrice));
        } else if (item.getStatus() == BookingStatus.CONFIRMED) {
            holder.binding.bookingPaymentPrice.setText(context.getString(R.string.all_bookings_not_paid_yet, formattedPrice));
        } else {
            holder.binding.bookingPaymentPrice.setText(context.getString(R.string.all_bookings_not_paid, formattedPrice));
        }

        holder.binding.chipStatus.setText(item.getStatus().name());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminBookingBinding binding;

        BookingViewHolder(ItemAdminBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
