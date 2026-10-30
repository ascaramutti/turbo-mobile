package com.turbo.mobile.ui.owner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.TurboUi;

import java.util.Calendar;
import java.util.List;

/** W-14 My vehicles: one card per vehicle of the logged-in car owner. */
public class MyVehiclesFragment extends Fragment {

    private AppContainer container;
    private long ownerId;
    private LinearLayout vehicleList;
    private View emptyState;

    public MyVehiclesFragment() {
        super(R.layout.fragment_my_vehicles);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        vehicleList = view.findViewById(R.id.vehicle_list);
        emptyState = view.findViewById(R.id.empty_state);
        // Without a vehicle id the form opens as "Add vehicle".
        view.findViewById(R.id.add_vehicle).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VehicleFormActivity.class)));
    }

    // onResume runs every time this screen becomes visible, so the list is always up to date.
    @Override
    public void onResume() {
        super.onResume();
        loadVehicles();
    }

    // Repository calls run on a background thread (container.executor) so the screen never freezes.
    // Only the main thread may change the screen, so each result is sent back with TurboUi.runOnMainThread().

    private void loadVehicles() {
        container.executor.execute(() -> {
            List<OwnerVehicleItem> vehicles = container.vehicleRepository.getOwnerVehicles(ownerId);
            TurboUi.runOnMainThread(this, () -> showVehicles(vehicles));
        });
    }

    private void activate(OwnerVehicleItem item, long availableUntil) {
        container.executor.execute(() -> {
            Result<Void> result = container.vehicleRepository.publish(ownerId, item.getId(), availableUntil);
            TurboUi.runOnMainThread(this, () -> showErrorAndReload(result));
        });
    }

    private void deactivate(OwnerVehicleItem item) {
        container.executor.execute(() -> {
            Result<Void> result = container.vehicleRepository.unpublish(ownerId, item.getId());
            TurboUi.runOnMainThread(this, () -> showErrorAndReload(result));
        });
    }

    private void delete(OwnerVehicleItem item) {
        container.executor.execute(() -> {
            Result<Integer> result = container.vehicleRepository.deleteVehicle(ownerId, item.getId());
            TurboUi.runOnMainThread(this, () -> showErrorAndReload(result));
        });
    }

    private void showErrorAndReload(Result<?> result) {
        if (result instanceof Result.Error) {
            TurboUi.showError(requireContext(), result);
        }
        loadVehicles();
    }

    private void showVehicles(List<OwnerVehicleItem> vehicles) {
        vehicleList.removeAllViews();
        for (OwnerVehicleItem item : vehicles) {
            View card = getLayoutInflater().inflate(R.layout.item_owner_vehicle, vehicleList, false);
            fillCard(card, item);
            vehicleList.addView(card);
        }
        emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // What a card shows depends on the vehicle's badge (its status in the wireframe).
    private void fillCard(View card, OwnerVehicleItem item) {
        TextView title = card.findViewById(R.id.vehicle_title);
        TextView subtitle = card.findViewById(R.id.vehicle_subtitle);
        TextView primaryChip = card.findViewById(R.id.chip_primary);
        TextView secondaryChip = card.findViewById(R.id.chip_secondary);
        TextView toggle = card.findViewById(R.id.action_toggle);
        View toggleSeparator = card.findViewById(R.id.action_toggle_separator);

        title.setText(item.getTitle());

        switch (item.getBadge()) {
            case PUBLISHED:
                subtitle.setText(R.string.vehicle_approved_active);
                TurboUi.paintChip(primaryChip, getString(R.string.chip_approved),
                        R.color.turbo_success_bg, R.color.turbo_success);
                TurboUi.paintChip(secondaryChip, getString(R.string.chip_active),
                        R.color.turbo_success_bg, R.color.turbo_success);
                toggle.setText(R.string.action_deactivate);
                toggle.setOnClickListener(v -> deactivate(item));
                break;
            case NOT_PUBLISHED:
                subtitle.setText(R.string.vehicle_hidden_by_owner);
                TurboUi.paintChip(primaryChip, getString(R.string.chip_approved),
                        R.color.turbo_success_bg, R.color.turbo_success);
                TurboUi.paintChip(secondaryChip, getString(R.string.chip_inactive),
                        R.color.turbo_success_bg, R.color.turbo_success);
                toggle.setText(R.string.action_activate);
                toggle.setOnClickListener(v -> pickAvailableUntil(item));
                break;
            case PENDING_REVIEW:
                // "1 document pending" or "2 documents pending" (<plurals> in strings_owner.xml).
                int pending = item.getPendingDocuments();
                subtitle.setText(getResources().getQuantityString(R.plurals.vehicle_documents_pending, pending, pending));
                TurboUi.paintChip(primaryChip, getString(R.string.chip_pending_review),
                        R.color.turbo_warning_bg, R.color.turbo_warning);
                secondaryChip.setVisibility(View.GONE);
                toggle.setVisibility(View.GONE);
                toggleSeparator.setVisibility(View.GONE);
                break;
            case ACTION_NEEDED:
                // "Insurance rejected" or "Registration rejected".
                int documentName = R.string.document_insurance;
                if (item.getRejectedDocument() == DocumentType.REGISTRATION) {
                    documentName = R.string.document_registration;
                }
                subtitle.setText(getString(R.string.vehicle_document_rejected, getString(documentName)));
                TurboUi.paintChip(primaryChip, getString(R.string.chip_action_needed),
                        R.color.turbo_error_bg, R.color.turbo_error);
                TurboUi.paintChip(secondaryChip, getString(R.string.chip_not_visible),
                        R.color.turbo_neutral_bg, R.color.turbo_text_secondary);
                toggle.setVisibility(View.GONE);
                toggleSeparator.setVisibility(View.GONE);
                break;
        }

        card.findViewById(R.id.action_edit).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VehicleFormActivity.class);
            intent.putExtra(VehicleFormActivity.EXTRA_VEHICLE_ID, item.getId());
            startActivity(intent);
        });
        card.findViewById(R.id.action_documents).setOnClickListener(v -> {
            // "Ford Escape 2019 · GH7 89J · Burnaby", shown at the top of W-16.
            String summary = item.getTitle() + " · " + item.getPlate() + " · " + item.getZoneName();
            Intent intent = new Intent(requireContext(), VehicleDocumentsActivity.class);
            intent.putExtra(VehicleDocumentsActivity.EXTRA_VEHICLE_ID, item.getId());
            intent.putExtra(VehicleDocumentsActivity.EXTRA_VEHICLE_SUMMARY, summary);
            startActivity(intent);
        });
        card.findViewById(R.id.action_delete).setOnClickListener(v -> confirmDelete(item));
    }

    // Activating needs an "available until" date and W-14 has no field for it, so we ask with Android's date picker.
    private void pickAvailableUntil(OwnerVehicleItem item) {
        DatePickerDialog picker = new DatePickerDialog(requireContext()); // opens on today
        picker.getDatePicker().setMinDate(System.currentTimeMillis()); // past days can't be picked
        picker.setOnDateSetListener((view, year, month, day) -> {
            Calendar until = Calendar.getInstance();
            until.set(year, month, day, 23, 59, 0); // until the end of the chosen day
            activate(item, until.getTimeInMillis());
        });
        picker.show();
    }

    private void confirmDelete(OwnerVehicleItem item) {
        TurboUi.showConfirmDialog(requireContext(), getString(R.string.delete_vehicle_title),
                getString(R.string.delete_vehicle_body, TurboUi.withoutYear(item.getTitle())),
                getString(R.string.cancel), getString(R.string.delete_vehicle), () -> delete(item));
    }
}
