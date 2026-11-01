package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.SearchService;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.databinding.FragmentSearchBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

/** W-05 Search vehicles: the published vehicles, filtered by zone, category, service and max price. */
public class SearchFragment extends Fragment {

    // Options of the "Max price" filter, in dollars per hour.
    private static final int[] MAX_PRICES = {10, 12, 15, 20};

    private FragmentSearchBinding binding;
    private AppContainer container;
    private final SearchFilter filter = new SearchFilter(); // starts empty = no filters
    private List<Zone> zones = new ArrayList<>();
    private SearchVehiclesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((TurboApp) requireActivity().getApplication()).getContainer();

        adapter = new SearchVehiclesAdapter(requireActivity(), vehicle -> {
            Intent intent = new Intent(requireContext(), VehicleDetailActivity.class);
            intent.putExtra(VehicleDetailActivity.EXTRA_VEHICLE_ID, vehicle.getId());
            startActivity(intent);
        });

        binding.recyclerVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerVehicles.setAdapter(adapter);

        binding.filterZone.setOnClickListener(v -> chooseZone());
        binding.filterCategory.setOnClickListener(v -> chooseCategory());
        binding.filterService.setOnClickListener(v -> chooseService());
        binding.filterPrice.setOnClickListener(v -> choosePrice());
        binding.clearFilters.setOnClickListener(v -> clearFilters());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        search();
    }

    // Reads the zones (the options of the zone filter) and the vehicles that match the filters.
    private void search() {
        container.executor.execute(() -> {
            List<Zone> zoneList = container.vehicleRepository.getZones();
            List<VehicleSummary> vehicles = container.vehicleRepository.searchPublished(filter);
            TurboUi.runOnMainThread(this, () -> {
                zones = zoneList;
                showVehicles(vehicles);
            });
        });
    }

    private void showVehicles(List<VehicleSummary> vehicles) {
        if (binding == null) {
            return;
        }
        adapter.setItems(vehicles);

        // "Burnaby · 3 results". Without a zone filter: "All zones · 3 results".
        String zone = getString(R.string.all_zones);
        if (filter.getZoneId() != null) {
            zone = binding.filterZone.getText().toString(); // the chip shows the chosen zone
        }
        binding.resultsBanner.setText(getResources().getQuantityString(R.plurals.search_results, vehicles.size(),
                zone, vehicles.size()));
        binding.resultsBanner.setVisibility(vehicles.isEmpty() ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Each filter shows a list. Position 0 of every list is "no filter" ("All zones", "Any price"...).

    private void chooseZone() {
        if (binding == null) return;
        String[] options = new String[zones.size() + 1];
        options[0] = getString(R.string.all_zones);
        for (int i = 0; i < zones.size(); i++) {
            options[i + 1] = zones.get(i).getName();
        }
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        filter.setZoneId(null);
                    } else {
                        filter.setZoneId(zones.get(which - 1).getId()); // -1 because of "All zones"
                    }
                    if (binding != null) {
                        updateChip(binding.filterZone, which, options[which], R.string.filter_zone);
                    }
                    search();
                })
                .show();
    }

    private void chooseCategory() {
        if (binding == null) return;
        // The categories of the vehicle form ("Select", "Sedan", "SUV"...), with "Any category" instead of "Select".
        String[] options = getResources().getStringArray(R.array.vehicle_categories);
        options[0] = getString(R.string.any_category);
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        filter.setCategory(null);
                    } else {
                        filter.setCategory(VehicleCategory.values()[which - 1]);
                    }
                    if (binding != null) {
                        updateChip(binding.filterCategory, which, options[which], R.string.filter_category);
                    }
                    search();
                })
                .show();
    }

    private void chooseService() {
        if (binding == null) return;
        // "Any service", "Taxi", "Delivery": the same order as SearchService (ANY, TAXI, DELIVERY).
        String[] options = getResources().getStringArray(R.array.search_services);
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    filter.setService(SearchService.values()[which]);
                    if (binding != null) {
                        updateChip(binding.filterService, which, options[which], R.string.filter_service);
                    }
                    search();
                })
                .show();
    }

    private void choosePrice() {
        if (binding == null) return;
        String[] options = new String[MAX_PRICES.length + 1];
        options[0] = getString(R.string.any_price);
        for (int i = 0; i < MAX_PRICES.length; i++) {
            options[i + 1] = getString(R.string.max_price_option, MAX_PRICES[i]); // "≤ CAD 10"
        }
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        filter.setMaxRateCents(null);
                    } else {
                        filter.setMaxRateCents(MAX_PRICES[which - 1] * 100); // dollars -> cents
                    }
                    if (binding != null) {
                        updateChip(binding.filterPrice, which, options[which], R.string.filter_max_price);
                    }
                    search();
                })
                .show();
    }

    // Without a filter the chip shows its name ("Zone"). With one, it shows the choice ("Burnaby") in orange.
    private void updateChip(TextView chip, int which, String choice, int filterName) {
        if (which == 0) {
            chip.setText(filterName);
            chip.setSelected(false);
        } else {
            chip.setText(choice);
            chip.setSelected(true);
        }
    }

    private void clearFilters() {
        if (binding == null) return;
        filter.setZoneId(null);
        filter.setCategory(null);
        filter.setService(null);
        filter.setMaxRateCents(null);
        updateChip(binding.filterZone, 0, null, R.string.filter_zone);
        updateChip(binding.filterCategory, 0, null, R.string.filter_category);
        updateChip(binding.filterService, 0, null, R.string.filter_service);
        updateChip(binding.filterPrice, 0, null, R.string.filter_max_price);
        search();
    }
}
