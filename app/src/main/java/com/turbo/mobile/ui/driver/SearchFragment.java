package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

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
import com.turbo.mobile.ui.ScreenSetup;

import java.util.List;

public class SearchFragment extends Fragment {

    private static final int[] MAX_PRICES = {10, 12, 15, 20};

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private VehicleAdapter adapter;
    private final SearchFilter filter = new SearchFilter();
    private List<Zone> zones;

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new SearchViewModel(appContainer.vehicleRepository);
        zones = viewModel.getZones();

        adapter = new VehicleAdapter(appContainer.imageStorage, this);
        binding.recyclerVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerVehicles.setAdapter(adapter);

        setUpZoneFilter();
        setUpCategoryFilter();
        setUpServiceFilter();
        setUpPriceFilter();
        binding.clearFilters.setOnClickListener(v -> clearFilters());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        search();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void openVehicle(VehicleSummary vehicle) {
        Intent intent = new Intent(requireContext(), VehicleDetailActivity.class);
        intent.putExtra(VehicleDetailActivity.EXTRA_VEHICLE_ID, vehicle.getId());
        startActivity(intent);
    }

    private void search() {
        List<VehicleSummary> vehicles = viewModel.search(filter);
        adapter.setItems(vehicles);

        String zone = binding.filterZone.getSelectedItem().toString();
        binding.resultsBanner.setText(getResources().getQuantityString(R.plurals.search_results, vehicles.size(), zone, vehicles.size()));
        binding.resultsBanner.setVisibility(vehicles.isEmpty() ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setUpZoneFilter() {
        String[] options = new String[zones.size() + 1];
        options[0] = getString(R.string.search_all_zones);
        for (int i = 0; i < zones.size(); i++) {
            options[i + 1] = zones.get(i).getName();
        }
        ScreenSetup.fillSpinner(binding.filterZone, options);
        binding.filterZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    filter.setZoneId(null);
                } else {
                    filter.setZoneId(zones.get(position - 1).getId());
                }
                binding.filterZone.setSelected(position != 0);
                search();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpCategoryFilter() {
        String[] names = getResources().getStringArray(R.array.vehicle_category_names);
        String[] options = new String[names.length + 1];
        options[0] = getString(R.string.search_any_category);
        for (int i = 0; i < names.length; i++) {
            options[i + 1] = names[i];
        }
        ScreenSetup.fillSpinner(binding.filterCategory, options);
        binding.filterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    filter.setCategory(null);
                } else {
                    filter.setCategory(VehicleCategory.values()[position - 1]);
                }
                binding.filterCategory.setSelected(position != 0);
                search();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpServiceFilter() {
        String[] options = getResources().getStringArray(R.array.search_services);
        ScreenSetup.fillSpinner(binding.filterService, options);
        binding.filterService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter.setService(SearchService.values()[position]);
                binding.filterService.setSelected(position != 0);
                search();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpPriceFilter() {
        String[] options = new String[MAX_PRICES.length + 1];
        options[0] = getString(R.string.search_any_price);
        for (int i = 0; i < MAX_PRICES.length; i++) {
            options[i + 1] = getString(R.string.search_max_price_option, MAX_PRICES[i]);
        }
        ScreenSetup.fillSpinner(binding.filterPrice, options);
        binding.filterPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    filter.setMaxRateCents(null);
                } else {
                    filter.setMaxRateCents(MAX_PRICES[position - 1] * 100);
                }
                binding.filterPrice.setSelected(position != 0);
                search();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void clearFilters() {
        binding.filterZone.setSelection(0);
        binding.filterCategory.setSelection(0);
        binding.filterService.setSelection(0);
        binding.filterPrice.setSelection(0);
    }
}
