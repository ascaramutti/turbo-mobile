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
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.databinding.ItemOwnerVehicleBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.owner.fragments.MyVehiclesFragment;

import java.util.ArrayList;
import java.util.List;

public class OwnerVehicleAdapter extends RecyclerView.Adapter<OwnerVehicleAdapter.OwnerVehicleViewHolder> {

    private final MyVehiclesFragment fragment;
    private List<OwnerVehicleItem> vehicles = new ArrayList<>();

    public OwnerVehicleAdapter(MyVehiclesFragment fragment) {
        this.fragment = fragment;
    }

    public void setItems(List<OwnerVehicleItem> vehicles) {
        this.vehicles = vehicles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OwnerVehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOwnerVehicleBinding binding = ItemOwnerVehicleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new OwnerVehicleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerVehicleViewHolder holder, int position) {
        OwnerVehicleItem vehicle = vehicles.get(position);
        ItemOwnerVehicleBinding binding = holder.binding;
        Resources resources = binding.getRoot().getResources();

        binding.vehicleTitle.setText(vehicle.getTitle());
        binding.actionChangeDate.setVisibility(View.GONE);
        binding.chipSecondary.setVisibility(View.VISIBLE);
        binding.actionToggle.setVisibility(View.VISIBLE);
        binding.actionToggleSeparator.setVisibility(View.VISIBLE);

        switch (vehicle.getBadge()) {
            case PUBLISHED:
                binding.vehicleSubtitle.setText(resources.getString(R.string.my_vehicles_published_until, DisplayFormat.date(vehicle.getAvailableUntil())));
                binding.actionChangeDate.setVisibility(View.VISIBLE);
                binding.actionChangeDate.setOnClickListener(v -> fragment.openPublishDialog(vehicle));
                paintChip(binding.chipPrimary, R.string.my_vehicles_chip_approved, R.color.turbo_success_bg, R.color.turbo_success);
                paintChip(binding.chipSecondary, R.string.my_vehicles_chip_active, R.color.turbo_success_bg, R.color.turbo_success);
                binding.actionToggle.setText(R.string.my_vehicles_deactivate);
                binding.actionToggle.setOnClickListener(v -> fragment.deactivate(vehicle));
                break;
            case NOT_PUBLISHED:
                binding.vehicleSubtitle.setText(R.string.my_vehicles_hidden_by_owner);
                paintChip(binding.chipPrimary, R.string.my_vehicles_chip_approved, R.color.turbo_success_bg, R.color.turbo_success);
                paintChip(binding.chipSecondary, R.string.my_vehicles_chip_inactive, R.color.turbo_success_bg, R.color.turbo_success);
                binding.actionToggle.setText(R.string.my_vehicles_activate);
                binding.actionToggle.setOnClickListener(v -> fragment.openPublishDialog(vehicle));
                break;
            case PENDING_REVIEW:
                int pending = vehicle.getPendingDocuments();
                binding.vehicleSubtitle.setText(resources.getQuantityString(R.plurals.my_vehicles_documents_pending, pending, pending));
                paintChip(binding.chipPrimary, R.string.my_vehicles_chip_pending_review, R.color.turbo_warning_bg, R.color.turbo_warning);
                binding.chipSecondary.setVisibility(View.GONE);
                binding.actionToggle.setVisibility(View.GONE);
                binding.actionToggleSeparator.setVisibility(View.GONE);
                break;
            case ACTION_NEEDED:
                if (vehicle.getRejectedDocument() == DocumentType.REGISTRATION) {
                    binding.vehicleSubtitle.setText(R.string.my_vehicles_registration_rejected);
                } else {
                    binding.vehicleSubtitle.setText(R.string.my_vehicles_insurance_rejected);
                }
                paintChip(binding.chipPrimary, R.string.my_vehicles_chip_action_needed, R.color.turbo_error_bg, R.color.turbo_error);
                paintChip(binding.chipSecondary, R.string.my_vehicles_chip_not_visible, R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                binding.actionToggle.setVisibility(View.GONE);
                binding.actionToggleSeparator.setVisibility(View.GONE);
                break;
        }

        binding.actionDelete.setOnClickListener(v -> fragment.delete(vehicle));
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    private void paintChip(TextView chip, int text, int backgroundColor, int textColor) {
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(chip.getContext(), backgroundColor));
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), textColor));
    }

    static class OwnerVehicleViewHolder extends RecyclerView.ViewHolder {

        final ItemOwnerVehicleBinding binding;

        OwnerVehicleViewHolder(ItemOwnerVehicleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
