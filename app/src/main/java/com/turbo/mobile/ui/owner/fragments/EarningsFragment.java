package com.turbo.mobile.ui.owner.fragments;

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
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.FragmentEarningsBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.owner.adapters.EarningsAdapter;
import com.turbo.mobile.ui.owner.viewmodels.EarningsViewModel;

import java.util.List;

public class EarningsFragment extends Fragment {

    private FragmentEarningsBinding binding;
    private EarningsViewModel viewModel;
    private EarningsAdapter adapter;
    private List<OwnerVehicleItem> vehicles;
    private Long selectedVehicleId = null;

    public EarningsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEarningsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new EarningsViewModel(appContainer.paymentRepository, appContainer.vehicleRepository, appContainer.sessionManager);
        vehicles = viewModel.getVehicles();

        adapter = new EarningsAdapter();
        binding.recyclerEarnings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerEarnings.setAdapter(adapter);

        setUpVehicleFilter();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEarnings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpVehicleFilter() {
        String[] options = new String[vehicles.size() + 1];
        options[0] = getString(R.string.earnings_all_vehicles);
        for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
            options[vehicleIndex + 1] = vehicles.get(vehicleIndex).getTitle();
        }
        ScreenSetup.fillSpinner(binding.vehicleFilter, options);
        binding.vehicleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedVehicleId = null;
                } else {
                    selectedVehicleId = vehicles.get(position - 1).getId();
                }
                loadEarnings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadEarnings() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<OwnerEarnings> result = viewModel.getEarnings(selectedVehicleId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<OwnerEarnings>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            binding.content.setVisibility(View.GONE);
            return;
        }

        OwnerEarnings earnings = ((Result.Success<OwnerEarnings>) result).data;
        binding.totalEarned.setText(DisplayFormat.money(earnings.getTotalCents()));
        binding.completedRentals.setText(String.valueOf(earnings.getCompletedRentals()));
        binding.thisMonth.setText(DisplayFormat.money(earnings.getMonthCents()));
        adapter.setItems(earnings.getHistory());
        binding.emptyState.setVisibility(earnings.getHistory().isEmpty() ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(View.VISIBLE);
    }
}
