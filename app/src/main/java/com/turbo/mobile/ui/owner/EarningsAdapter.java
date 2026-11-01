package com.turbo.mobile.ui.owner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.EarningItem;
import com.turbo.mobile.databinding.ItemInfoCardBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

public class EarningsAdapter extends RecyclerView.Adapter<EarningsAdapter.EarningViewHolder> {

    private final List<EarningItem> items = new ArrayList<>();

    public void setItems(List<EarningItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EarningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoCardBinding binding = ItemInfoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EarningViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EarningViewHolder holder, int position) {
        EarningItem earning = items.get(position);
        ItemInfoCardBinding b = holder.binding;
        Context context = holder.itemView.getContext();

        b.cardActions.setVisibility(View.GONE);
        b.cardThumbnail.setVisibility(View.GONE);
        b.chipSecondary.setVisibility(View.GONE);
        b.cardFooter.setVisibility(View.VISIBLE);

        String vehicleAndDriver = TurboUi.withoutYear(earning.getVehicleTitle()) + " · " + earning.getDriverName();
        String when = TurboUi.date(earning.getPaidAt()) + " · " + earning.getHours() + " h";

        b.cardTitle.setText(vehicleAndDriver);
        b.cardSubtitle.setText(when);
        TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_paid), R.color.turbo_success_bg, R.color.turbo_success);
        b.cardFooter.setText(context.getString(R.string.payout, TurboUi.money(earning.getPayoutCents())));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EarningViewHolder extends RecyclerView.ViewHolder {
        final ItemInfoCardBinding binding;

        EarningViewHolder(ItemInfoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
