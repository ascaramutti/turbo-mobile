package com.turbo.mobile.ui.driver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.databinding.ItemInfoCardBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

public class DriverBookingsAdapter extends RecyclerView.Adapter<DriverBookingsAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(BookingListItem booking);
    }

    private final OnBookingClickListener listener;
    private final List<BookingListItem> items = new ArrayList<>();
    private int currentTab = R.id.tab_upcoming;

    public DriverBookingsAdapter(OnBookingClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<BookingListItem> newItems, int currentTab) {
        this.currentTab = currentTab;
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoCardBinding binding = ItemInfoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingListItem booking = items.get(position);
        ItemInfoCardBinding b = holder.binding;
        Context context = holder.itemView.getContext();

        b.cardActions.setVisibility(View.GONE);
        b.cardThumbnail.setVisibility(View.GONE);
        b.chipSecondary.setVisibility(View.GONE);

        b.cardTitle.setText(TurboUi.withoutYear(booking.getVehicleTitle()));
        if (currentTab == R.id.tab_history) {
            String hours = context.getResources().getQuantityString(R.plurals.hours_long, booking.getHours(), booking.getHours());
            b.cardSubtitle.setText(TurboUi.date(booking.getStart()) + " · " + hours);
        } else {
            b.cardSubtitle.setText(TurboUi.date(booking.getStart()) + " · "
                    + TurboUi.timeRange(booking.getStart(), booking.getEnd()));
        }

        BookingChips.paintStatus(b.chipPrimary, booking);

        if (BookingChips.isExpired(booking)) {
            b.cardFooter.setText(R.string.no_owner_response);
        } else if (booking.getStatus() == BookingStatus.PENDING) {
            b.cardFooter.setText(R.string.waiting_for_owner);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            BookingChips.paintPayment(b.chipSecondary, booking);
            if (booking.isPaid()) {
                b.cardFooter.setText(R.string.ready_to_start);
            } else {
                b.cardFooter.setText(context.getString(R.string.pay_now, TurboUi.money(booking.getTotalPriceCents())));
            }
        } else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            BookingChips.paintPayment(b.chipSecondary, booking);
            b.cardFooter.setText(context.getString(R.string.trip_ends_at, TurboUi.time(booking.getEnd())));
        } else if (booking.getStatus() == BookingStatus.COMPLETED) {
            BookingChips.paintPayment(b.chipSecondary, booking);
            b.cardFooter.setText(TurboUi.money(booking.getTotalPriceCents()));
        } else if (booking.getStatus() == BookingStatus.REJECTED) {
            b.cardFooter.setText(R.string.request_declined);
        } else { // CANCELLED
            b.cardFooter.setText(R.string.cancelled_before_payment);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookingClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        final ItemInfoCardBinding binding;

        BookingViewHolder(ItemInfoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
