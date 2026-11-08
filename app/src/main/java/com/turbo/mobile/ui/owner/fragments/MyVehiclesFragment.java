package com.turbo.mobile.ui.owner.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.databinding.FragmentMyVehiclesBinding;
import com.turbo.mobile.ui.owner.VehicleFormActivity;
import com.turbo.mobile.ui.owner.adapters.OwnerVehicleAdapter;
import com.turbo.mobile.ui.owner.viewmodels.MyVehiclesViewModel;

import java.util.List;

public class MyVehiclesFragment extends Fragment {

    private FragmentMyVehiclesBinding binding;
    private MyVehiclesViewModel viewModel;
    private OwnerVehicleAdapter adapter;

    public MyVehiclesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyVehiclesBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new MyVehiclesViewModel(appContainer.vehicleRepository, appContainer.sessionManager);

        adapter = new OwnerVehicleAdapter(this);
        binding.recyclerVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerVehicles.setAdapter(adapter);

        binding.addVehicle.setOnClickListener(v -> startActivity(new Intent(requireContext(), VehicleFormActivity.class)));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadVehicles();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void openEditForm(OwnerVehicleItem vehicle) {
        Intent intent = new Intent(requireContext(), VehicleFormActivity.class);
        intent.putExtra(VehicleFormActivity.EXTRA_VEHICLE_ID, vehicle.getId());
        startActivity(intent);
    }

    public void openPublishDialog(OwnerVehicleItem vehicle) {
        hideBanners();
        Bundle arguments = new Bundle();
        arguments.putLong(PublishVehicleDialogFragment.ARG_VEHICLE_ID, vehicle.getId());
        arguments.putString(PublishVehicleDialogFragment.ARG_VEHICLE_TITLE, vehicle.getTitle());
        PublishVehicleDialogFragment dialog = new PublishVehicleDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getChildFragmentManager(), "publish_vehicle");
    }

    public void deactivate(OwnerVehicleItem vehicle) {
        hideBanners();
        Result<Void> result = viewModel.unpublish(vehicle.getId());
        if (result instanceof Result.Error) {
            showError(((Result.Error<Void>) result).code);
        }
        loadVehicles();
    }

    public void delete(OwnerVehicleItem vehicle) {
        hideBanners();
        Result<Integer> result = viewModel.deleteVehicle(vehicle.getId());
        if (result instanceof Result.Success) {
            int rejected = ((Result.Success<Integer>) result).data;
            binding.successBanner.setText(getResources().getQuantityString(R.plurals.my_vehicles_deleted, rejected, rejected));
            binding.successBanner.setVisibility(View.VISIBLE);
        } else {
            showError(((Result.Error<Integer>) result).code);
        }
        loadVehicles();
    }

    public void loadVehicles() {
        List<OwnerVehicleItem> vehicles = viewModel.getVehicles();
        adapter.setItems(vehicles);
        binding.emptyState.setVisibility(vehicles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showError(RepoError code) {
        int message = R.string.error_unexpected;
        if (code == VehicleError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == VehicleError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        } else if (code == VehicleError.HAS_ACTIVE_BOOKINGS) {
            message = R.string.my_vehicles_error_active_bookings;
        }
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }

    private void hideBanners() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);
    }
}
