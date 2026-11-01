package com.turbo.mobile.ui.driver;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.databinding.ItemInfoCardBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchVehiclesAdapter extends RecyclerView.Adapter<SearchVehiclesAdapter.SearchVehicleViewHolder> {

    public interface OnVehicleClickListener {
        void onVehicleClick(VehicleSummary vehicle);
    }

    private final Activity activity;
    private final OnVehicleClickListener listener;
    private final List<VehicleSummary> items = new ArrayList<>();

    public SearchVehiclesAdapter(Activity activity, OnVehicleClickListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void setItems(List<VehicleSummary> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchVehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInfoCardBinding binding = ItemInfoCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SearchVehicleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchVehicleViewHolder holder, int position) {
        VehicleSummary vehicle = items.get(position);
        ItemInfoCardBinding b = holder.binding;
        Context context = holder.itemView.getContext();

        b.cardActions.setVisibility(View.GONE);
        b.cardThumbnail.setVisibility(View.GONE);

        b.cardTitle.setText(vehicle.getMake() + " " + vehicle.getModel() + " " + vehicle.getYear());
        b.cardSubtitle.setText(TurboUi.categoryName(context, vehicle.getCategory()) + " · "
                + TurboUi.serviceName(context, vehicle.getServiceType()));

        String price = context.getString(R.string.price_per_hour, TurboUi.money(vehicle.getHourlyRateCents()));
        TurboUi.paintChip(b.chipPrimary, price, R.color.turbo_primary_container, R.color.turbo_primary);

        if (vehicle.getOwnerRatingAvg() != null) {
            String rating = "★ " + String.format(Locale.US, "%.1f", vehicle.getOwnerRatingAvg());
            TurboUi.paintChip(b.chipSecondary, rating, R.color.turbo_primary_container, R.color.turbo_primary);
        } else {
            b.chipSecondary.setVisibility(View.GONE);
        }

        b.cardFooter.setText(vehicle.getZoneName());

        if (vehicle.getThumbnailPath() != null && !vehicle.getThumbnailPath().isEmpty() && activity != null) {
            TurboUi.showCardPhoto(activity, b.getRoot(), vehicle.getThumbnailPath());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVehicleClick(vehicle);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SearchVehicleViewHolder extends RecyclerView.ViewHolder {
        final ItemInfoCardBinding binding;

        SearchVehicleViewHolder(ItemInfoCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
