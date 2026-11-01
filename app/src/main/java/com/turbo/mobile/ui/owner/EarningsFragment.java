package com.turbo.mobile.ui.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentEarningsBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

/** W-19 Earnings: totals and paid shifts, for all the owner's vehicles or just one. */
public class EarningsFragment extends Fragment {

    private FragmentEarningsBinding binding;
    private AppContainer container;
    private long ownerId;
    private Long vehicleId = null; // vehicle chosen in the filter; null = all vehicles
    private List<OwnerVehicleItem> vehicles = new ArrayList<>();
    private EarningsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEarningsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();

        adapter = new EarningsAdapter();
        binding.recyclerEarnings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerEarnings.setAdapter(adapter);

        binding.vehicleFilter.setOnClickListener(v -> chooseVehicle());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEarnings();
    }

    // Reads the earnings and also the owner's vehicles (the options of the filter).
    private void loadEarnings() {
        Long filter = vehicleId;
        container.executor.execute(() -> {
            List<OwnerVehicleItem> ownerVehicles = container.vehicleRepository.getOwnerVehicles(ownerId);
            Result<OwnerEarnings> result = container.paymentRepository.getOwnerEarnings(ownerId, filter);
            TurboUi.runOnMainThread(this, () -> {
                vehicles = ownerVehicles;
                if (result instanceof Result.Success) {
                    showEarnings(((Result.Success<OwnerEarnings>) result).data);
                } else {
                    TurboUi.showError(requireContext(), result);
                }
            });
        });
    }

    private void showEarnings(OwnerEarnings earnings) {
        if (binding == null) return;
        binding.totalEarned.setText(TurboUi.moneyWithoutCents(earnings.getTotalCents()));
        binding.completedShifts.setText(String.valueOf(earnings.getCompletedRentals()));
        binding.thisMonth.setText(TurboUi.moneyWithoutCents(earnings.getMonthCents()));

        adapter.setItems(earnings.getHistory());
        binding.earningsEmpty.setVisibility(earnings.getHistory().isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Shows a list: "All vehicles" plus one row per vehicle. Choosing one reloads the earnings.
    private void chooseVehicle() {
        if (binding == null) return;
        String[] options = new String[vehicles.size() + 1];
        options[0] = getString(R.string.all_vehicles);
        for (int i = 0; i < vehicles.size(); i++) {
            options[i + 1] = vehicles.get(i).getTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    // which = the position tapped. 0 is "All vehicles", so the vehicles start at 1.
                    if (which == 0) {
                        vehicleId = null;
                    } else {
                        vehicleId = vehicles.get(which - 1).getId();
                    }
                    if (binding != null) {
                        binding.vehicleFilter.setText(options[which]);
                    }
                    loadEarnings();
                })
                .show();
    }
}
