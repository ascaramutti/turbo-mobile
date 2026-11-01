package com.turbo.mobile.ui.owner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.databinding.ItemOwnerVehicleBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

public class OwnerVehiclesAdapter extends RecyclerView.Adapter<OwnerVehiclesAdapter.VehicleViewHolder> {

    public interface OnVehicleActionListener {
        void onEdit(OwnerVehicleItem item);
        void onToggle(OwnerVehicleItem item);
        void onDocuments(OwnerVehicleItem item);
        void onDelete(OwnerVehicleItem item);
    }

    private final List<OwnerVehicleItem> items = new ArrayList<>();
    private final OnVehicleActionListener listener;

    public OwnerVehiclesAdapter(OnVehicleActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<OwnerVehicleItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOwnerVehicleBinding binding = ItemOwnerVehicleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VehicleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        OwnerVehicleItem item = items.get(position);
        ItemOwnerVehicleBinding b = holder.binding;
        Context context = holder.itemView.getContext();

        b.vehicleTitle.setText(item.getTitle());

        // Reset visibility
        b.chipSecondary.setVisibility(View.VISIBLE);
        b.actionToggle.setVisibility(View.VISIBLE);
        b.actionToggleSeparator.setVisibility(View.VISIBLE);

        switch (item.getBadge()) {
            case PUBLISHED:
                b.vehicleSubtitle.setText(R.string.vehicle_approved_active);
                TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_approved),
                        R.color.turbo_success_bg, R.color.turbo_success);
                TurboUi.paintChip(b.chipSecondary, context.getString(R.string.chip_active),
                        R.color.turbo_success_bg, R.color.turbo_success);
                b.actionToggle.setText(R.string.action_deactivate);
                break;
            case NOT_PUBLISHED:
                b.vehicleSubtitle.setText(R.string.vehicle_hidden_by_owner);
                TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_approved),
                        R.color.turbo_success_bg, R.color.turbo_success);
                TurboUi.paintChip(b.chipSecondary, context.getString(R.string.chip_inactive),
                        R.color.turbo_success_bg, R.color.turbo_success);
                b.actionToggle.setText(R.string.action_activate);
                break;
            case PENDING_REVIEW:
                int pending = item.getPendingDocuments();
                b.vehicleSubtitle.setText(context.getResources().getQuantityString(R.plurals.vehicle_documents_pending, pending, pending));
                TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_pending_review),
                        R.color.turbo_warning_bg, R.color.turbo_warning);
                b.chipSecondary.setVisibility(View.GONE);
                b.actionToggle.setVisibility(View.GONE);
                b.actionToggleSeparator.setVisibility(View.GONE);
                break;
            case ACTION_NEEDED:
                int documentName = R.string.document_insurance;
                if (item.getRejectedDocument() == DocumentType.REGISTRATION) {
                    documentName = R.string.document_registration;
                }
                b.vehicleSubtitle.setText(context.getString(R.string.vehicle_document_rejected, context.getString(documentName)));
                TurboUi.paintChip(b.chipPrimary, context.getString(R.string.chip_action_needed),
                        R.color.turbo_error_bg, R.color.turbo_error);
                TurboUi.paintChip(b.chipSecondary, context.getString(R.string.chip_not_visible),
                        R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                b.actionToggle.setVisibility(View.GONE);
                b.actionToggleSeparator.setVisibility(View.GONE);
                break;
        }

        b.actionEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });
        b.actionToggle.setOnClickListener(v -> {
            if (listener != null) listener.onToggle(item);
        });
        b.actionDocuments.setOnClickListener(v -> {
            if (listener != null) listener.onDocuments(item);
        });
        b.actionDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        final ItemOwnerVehicleBinding binding;

        VehicleViewHolder(ItemOwnerVehicleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
