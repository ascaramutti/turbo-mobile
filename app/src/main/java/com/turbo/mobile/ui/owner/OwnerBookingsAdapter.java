package com.turbo.mobile.ui.owner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.databinding.ItemInfoCardBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OwnerBookingsAdapter extends RecyclerView.Adapter<OwnerBookingsAdapter.OwnerBookingViewHolder> {

    public interface OnBookingActionListener {
        void onReject(BookingListItem booking);
        void onConfirm(BookingListItem booking);
        void onReview(BookingListItem booking);
    }

    private final OnBookingActionListener listener;
    private final List<BookingListItem> items = new ArrayList<>();
    private int currentTab = R.id.tab_requests;

    public OwnerBookingsAdapter(OnBookingActionListener listener) {
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
    public OwnerBookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoCardBinding binding = ItemInfoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new OwnerBookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerBookingViewHolder holder, int position) {
        BookingListItem booking = items.get(position);
        ItemInfoCardBinding b = holder.binding;
        Context context = holder.itemView.getContext();

        // Reset default states
        b.cardActions.setVisibility(View.GONE);
        b.cardThumbnail.setVisibility(View.GONE);
        b.cardFooter.setVisibility(View.VISIBLE);
        b.cardFooter.setOnClickListener(null);
        b.chipSecondary.setVisibility(View.GONE);

        if (currentTab == R.id.tab_requests) {
            fillRequestCard(b, booking, context);
        } else if (currentTab == R.id.tab_upcoming) {
            fillUpcomingCard(b, booking, context);
        } else {
            fillHistoryCard(b, booking, context);
        }
    }

    private void fillRequestCard(ItemInfoCardBinding b, BookingListItem booking, Context context) {
        String name = booking.getDriverName();
        if (booking.getCounterpartRatingAvg() != null) {
            name = name + " · ★ " + String.format(Locale.US, "%.1f", booking.getCounterpartRatingAvg());
        }
        b.cardTitle.setText(name);
        b.cardSubtitle.setText(shift(booking));

        String service = context.getString(R.string.service_delivery);
        if (booking.getServiceType() == ServiceType.TAXI) {
            service = context.getString(R.string.service_taxi);
        }
        TurboUi.paintChip(b.chipPrimary, service, R.color.turbo_primary_container, R.color.turbo_primary);
        TurboUi.paintChip(b.chipSecondary, TurboUi.money(booking.getTotalPriceCents()),
                R.color.turbo_primary_container, R.color.turbo_primary);

        b.cardFooter.setVisibility(View.GONE);
        b.cardActions.setVisibility(View.VISIBLE);
        b.actionReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(booking);
        });
        b.actionConfirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirm(booking);
        });
    }

    private void fillUpcomingCard(ItemInfoCardBinding b, BookingListItem booking, Context context) {
        b.cardTitle.setText(booking.getDriverName());
        b.cardSubtitle.setText(shift(booking));

        String status = context.getString(R.string.chip_confirmed);
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            status = context.getString(R.string.chip_in_progress);
        }
        TurboUi.paintChip(b.chipPrimary, status, R.color.turbo_success_bg, R.color.turbo_success);

        String price = TurboUi.money(booking.getTotalPriceCents());
        if (booking.isPaid()) {
            TurboUi.paintChip(b.chipSecondary, context.getString(R.string.chip_paid),
                    R.color.turbo_success_bg, R.color.turbo_success);
            int ownerShare = booking.getTotalPriceCents() * 80 / 100;
            b.cardFooter.setText(context.getString(R.string.price_with_share, price, TurboUi.money(ownerShare)));
        } else {
            TurboUi.paintChip(b.chipSecondary, context.getString(R.string.chip_payment_pending),
                    R.color.turbo_warning_bg, R.color.turbo_warning);
            b.cardFooter.setText(price);
        }
    }

    private void fillHistoryCard(ItemInfoCardBinding b, BookingListItem booking, Context context) {
        b.cardTitle.setText(booking.getDriverName());
        String when = TurboUi.withoutYear(booking.getVehicleTitle()) + " · " + TurboUi.date(booking.getStart())
                + " · " + booking.getHours() + " h";
        b.cardSubtitle.setText(when);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_completed),
                    R.color.turbo_success_bg, R.color.turbo_success);
            String payment = context.getString(R.string.chip_not_paid);
            if (booking.isPaid()) {
                payment = context.getString(R.string.chip_paid);
            }
            TurboUi.paintChip(b.chipSecondary, payment, R.color.turbo_success_bg, R.color.turbo_success);
            if (booking.getOwnerActions().contains(OwnerAction.REVIEW)) {
                b.cardFooter.setText(R.string.review_driver);
                b.cardFooter.setOnClickListener(v -> {
                    if (listener != null) listener.onReview(booking);
                });
            } else {
                b.cardFooter.setText(R.string.driver_reviewed);
            }
        } else {
            String status = context.getString(R.string.chip_rejected);
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                status = context.getString(R.string.chip_cancelled);
            }
            TurboUi.paintChip(b.chipPrimary, status, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
            b.cardFooter.setText(R.string.history_preserved);
        }
    }

    private String shift(BookingListItem booking) {
        return TurboUi.withoutYear(booking.getVehicleTitle()) + " · " + TurboUi.date(booking.getStart())
                + " · " + TurboUi.time(booking.getStart()) + "–" + TurboUi.time(booking.getEnd());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OwnerBookingViewHolder extends RecyclerView.ViewHolder {
        final ItemInfoCardBinding binding;

        OwnerBookingViewHolder(ItemInfoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
