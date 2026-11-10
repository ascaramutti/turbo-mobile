package com.turbo.mobile.ui.owner.adapters;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.EarningItem;
import com.turbo.mobile.databinding.ItemEarningBinding;
import com.turbo.mobile.ui.DisplayFormat;

import java.util.ArrayList;
import java.util.List;

public class EarningsAdapter extends RecyclerView.Adapter<EarningsAdapter.EarningViewHolder> {

    private List<EarningItem> earnings = new ArrayList<>();

    public void setItems(List<EarningItem> earnings) {
        this.earnings = earnings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EarningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEarningBinding binding = ItemEarningBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new EarningViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EarningViewHolder holder, int position) {
        EarningItem earning = earnings.get(position);
        ItemEarningBinding binding = holder.binding;
        Resources resources = binding.getRoot().getResources();

        binding.vehicleAndDriver.setText(resources.getString(R.string.earnings_vehicle_and_driver, earning.getVehicleTitle(), earning.getDriverName()));
        binding.paidDay.setText(resources.getString(R.string.earnings_day_and_hours, DisplayFormat.date(earning.getPaidAt()), earning.getHours()));
        binding.payout.setText(resources.getString(R.string.earnings_payout, DisplayFormat.money(earning.getPayoutCents())));
    }

    @Override
    public int getItemCount() {
        return earnings.size();
    }

    static class EarningViewHolder extends RecyclerView.ViewHolder {

        final ItemEarningBinding binding;

        EarningViewHolder(ItemEarningBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
