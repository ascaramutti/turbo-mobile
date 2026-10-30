package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.SearchService;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** W-05 Search vehicles: the published vehicles, filtered by zone, category, service and max price. */
public class SearchFragment extends Fragment {

    // Options of the "Max price" filter, in dollars per hour.
    private static final int[] MAX_PRICES = {10, 12, 15, 20};

    private AppContainer container;
    private final SearchFilter filter = new SearchFilter(); // starts empty = no filters
    private List<Zone> zones = new ArrayList<>();

    private TextView zoneChip;
    private TextView categoryChip;
    private TextView serviceChip;
    private TextView priceChip;
    private TextView resultsBanner;
    private LinearLayout vehicleList;
    private View emptyState;

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        container = ((TurboApp) requireActivity().getApplication()).getContainer();
        zoneChip = view.findViewById(R.id.filter_zone);
        categoryChip = view.findViewById(R.id.filter_category);
        serviceChip = view.findViewById(R.id.filter_service);
        priceChip = view.findViewById(R.id.filter_price);
        resultsBanner = view.findViewById(R.id.results_banner);
        vehicleList = view.findViewById(R.id.vehicle_list);
        emptyState = view.findViewById(R.id.empty_state);

        zoneChip.setOnClickListener(v -> chooseZone());
        categoryChip.setOnClickListener(v -> chooseCategory());
        serviceChip.setOnClickListener(v -> chooseService());
        priceChip.setOnClickListener(v -> choosePrice());
        view.findViewById(R.id.clear_filters).setOnClickListener(v -> clearFilters());
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
        vehicleList.removeAllViews();
        for (VehicleSummary vehicle : vehicles) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, vehicleList, false);
            fillCard(card, vehicle);
            vehicleList.addView(card);
        }

        // "Burnaby · 3 results". Without a zone filter: "All zones · 3 results".
        String zone = getString(R.string.all_zones);
        if (filter.getZoneId() != null) {
            zone = zoneChip.getText().toString(); // the chip shows the chosen zone
        }
        resultsBanner.setText(getResources().getQuantityString(R.plurals.search_results, vehicles.size(),
                zone, vehicles.size()));
        resultsBanner.setVisibility(vehicles.isEmpty() ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // "Toyota Corolla 2022", "Sedan · Taxi & Delivery", the price and the owner's rating, the zone and the photo.
    private void fillCard(View card, VehicleSummary vehicle) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView priceText = card.findViewById(R.id.chip_primary);
        TextView ratingText = card.findViewById(R.id.chip_secondary);
        TextView footer = card.findViewById(R.id.card_footer);

        title.setText(vehicle.getMake() + " " + vehicle.getModel() + " " + vehicle.getYear());
        subtitle.setText(TurboUi.categoryName(requireContext(), vehicle.getCategory()) + " · "
                + TurboUi.serviceName(requireContext(), vehicle.getServiceType()));
        String price = getString(R.string.price_per_hour, TurboUi.money(vehicle.getHourlyRateCents()));
        TurboUi.paintChip(priceText, price, R.color.turbo_primary_container, R.color.turbo_primary);
        if (vehicle.getOwnerRatingAvg() != null) { // null while the owner has no reviews
            String rating = "★ " + String.format(Locale.US, "%.1f", vehicle.getOwnerRatingAvg());
            TurboUi.paintChip(ratingText, rating, R.color.turbo_primary_container, R.color.turbo_primary);
        }
        footer.setText(vehicle.getZoneName());
        TurboUi.showCardPhoto(requireActivity(), card, vehicle.getThumbnailPath());

        card.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VehicleDetailActivity.class);
            intent.putExtra(VehicleDetailActivity.EXTRA_VEHICLE_ID, vehicle.getId());
            startActivity(intent);
        });
    }

    // Each filter shows a list. Position 0 of every list is "no filter" ("All zones", "Any price"...).

    private void chooseZone() {
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
                    updateChip(zoneChip, which, options[which], R.string.filter_zone);
                    search();
                })
                .show();
    }

    private void chooseCategory() {
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
                    updateChip(categoryChip, which, options[which], R.string.filter_category);
                    search();
                })
                .show();
    }

    private void chooseService() {
        // "Any service", "Taxi", "Delivery": the same order as SearchService (ANY, TAXI, DELIVERY).
        String[] options = getResources().getStringArray(R.array.search_services);
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    filter.setService(SearchService.values()[which]);
                    updateChip(serviceChip, which, options[which], R.string.filter_service);
                    search();
                })
                .show();
    }

    private void choosePrice() {
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
                    updateChip(priceChip, which, options[which], R.string.filter_max_price);
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
        filter.setZoneId(null);
        filter.setCategory(null);
        filter.setService(null);
        filter.setMaxRateCents(null);
        updateChip(zoneChip, 0, null, R.string.filter_zone);
        updateChip(categoryChip, 0, null, R.string.filter_category);
        updateChip(serviceChip, 0, null, R.string.filter_service);
        updateChip(priceChip, 0, null, R.string.filter_max_price);
        search();
    }
}
