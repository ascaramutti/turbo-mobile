package com.turbo.mobile.ui.driver;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.databinding.ItemVehicleBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.util.ImageStorage;

import java.util.ArrayList;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private final ImageStorage imageStorage;
    private List<VehicleSummary> vehicles = new ArrayList<>();

    public VehicleAdapter(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    public void setItems(List<VehicleSummary> vehicles) {
        this.vehicles = vehicles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVehicleBinding binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.vehicleThumbnail.setClipToOutline(true);
        return new VehicleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        VehicleSummary vehicle = vehicles.get(position);
        ItemVehicleBinding binding = holder.binding;
        Resources resources = binding.getRoot().getResources();

        String category = resources.getStringArray(R.array.vehicle_category_names)[vehicle.getCategory().ordinal()];
        String service = resources.getStringArray(R.array.vehicle_service_names)[vehicle.getServiceType().ordinal()];
        binding.vehicleTitle.setText(vehicle.getMake() + " " + vehicle.getModel() + " " + vehicle.getYear());
        binding.vehicleType.setText(category + " · " + service);
        binding.vehiclePrice.setText(resources.getString(R.string.search_price_per_hour, DisplayFormat.money(vehicle.getHourlyRateCents())));
        binding.vehicleZone.setText(vehicle.getZoneName());

        if (vehicle.getOwnerRatingAvg() == null) {
            binding.vehicleRating.setVisibility(View.GONE);
        } else {
            binding.vehicleRating.setText(resources.getString(R.string.search_rating, vehicle.getOwnerRatingAvg()));
            binding.vehicleRating.setVisibility(View.VISIBLE);
        }

        binding.vehiclePhoto.setImageBitmap(imageStorage.load(vehicle.getThumbnailPath(), 400));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {

        final ItemVehicleBinding binding;

        VehicleViewHolder(ItemVehicleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
