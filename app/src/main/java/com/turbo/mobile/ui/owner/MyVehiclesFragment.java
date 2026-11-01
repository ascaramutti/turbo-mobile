package com.turbo.mobile.ui.owner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentMyVehiclesBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.Calendar;
import java.util.List;

/** W-14 My vehicles: one card per vehicle of the logged-in car owner. */
public class MyVehiclesFragment extends Fragment implements OwnerVehiclesAdapter.OnVehicleActionListener {

    private FragmentMyVehiclesBinding binding;
    private AppContainer container;
    private long ownerId;
    private OwnerVehiclesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyVehiclesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();

        adapter = new OwnerVehiclesAdapter(this);
        binding.recyclerMyVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMyVehicles.setAdapter(adapter);

        binding.addVehicle.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VehicleFormActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadVehicles();
    }

    private void loadVehicles() {
        container.executor.execute(() -> {
            List<OwnerVehicleItem> vehicles = container.vehicleRepository.getOwnerVehicles(ownerId);
            TurboUi.runOnMainThread(this, () -> showVehicles(vehicles));
        });
    }

    private void showVehicles(List<OwnerVehicleItem> vehicles) {
        if (binding == null) return;
        adapter.setItems(vehicles);
        binding.emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(OwnerVehicleItem item) {
        Intent intent = new Intent(requireContext(), VehicleFormActivity.class);
        intent.putExtra(VehicleFormActivity.EXTRA_VEHICLE_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onToggle(OwnerVehicleItem item) {
        if (item.getBadge() == com.turbo.mobile.data.model.VehicleBadge.PUBLISHED) {
            deactivate(item);
        } else {
            pickAvailableUntil(item);
        }
    }

    @Override
    public void onDocuments(OwnerVehicleItem item) {
        String summary = item.getTitle() + " · " + item.getPlate() + " · " + item.getZoneName();
        Intent intent = new Intent(requireContext(), VehicleDocumentsActivity.class);
        intent.putExtra(VehicleDocumentsActivity.EXTRA_VEHICLE_ID, item.getId());
        intent.putExtra(VehicleDocumentsActivity.EXTRA_VEHICLE_SUMMARY, summary);
        startActivity(intent);
    }

    @Override
    public void onDelete(OwnerVehicleItem item) {
        confirmDelete(item);
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

    private void confirmDelete(OwnerVehicleItem item) {
        TurboUi.showConfirmDialog(requireActivity(),
                getString(R.string.delete_vehicle_title),
                getString(R.string.delete_vehicle_body, TurboUi.withoutYear(item.getTitle())),
                getString(R.string.cancel),
                getString(R.string.delete_vehicle),
                () -> delete(item));
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

    private void pickAvailableUntil(OwnerVehicleItem item) {
        DatePickerDialog picker = new DatePickerDialog(requireContext());
        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.setOnDateSetListener((view, year, month, day) -> {
            Calendar until = Calendar.getInstance();
            until.set(year, month, day, 23, 59, 59);
            activate(item, until.getTimeInMillis());
        });
        picker.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
