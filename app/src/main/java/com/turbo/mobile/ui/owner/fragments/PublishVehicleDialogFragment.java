package com.turbo.mobile.ui.owner.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.databinding.DialogPublishVehicleBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.owner.viewmodels.MyVehiclesViewModel;

public class PublishVehicleDialogFragment extends DialogFragment {

    public static final String ARG_VEHICLE_ID = "vehicle_id";
    public static final String ARG_VEHICLE_TITLE = "vehicle_title";

    private static final int DAYS_AHEAD = 30;
    private static final int SIX_PM = 36;

    private DialogPublishVehicleBinding binding;
    private MyVehiclesViewModel viewModel;

    public PublishVehicleDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogPublishVehicleBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new MyVehiclesViewModel(appContainer.vehicleRepository, appContainer.sessionManager);

        binding.vehicleTitle.setText(requireArguments().getString(ARG_VEHICLE_TITLE));

        String[] days = new String[DAYS_AHEAD];
        for (int i = 0; i < DAYS_AHEAD; i++) {
            days[i] = DisplayFormat.dayAndDate(viewModel.getAvailableUntil(i, 0));
        }
        ScreenSetup.fillSpinner(binding.publishDay, days);
        binding.publishDay.setSelection(1);

        ScreenSetup.fillSpinner(binding.publishTime, DisplayFormat.halfHours());
        binding.publishTime.setSelection(SIX_PM);

        binding.cancel.setOnClickListener(v -> dismiss());
        binding.publish.setOnClickListener(v -> publish());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        requireDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void publish() {
        binding.errorBanner.setVisibility(View.GONE);
        long vehicleId = requireArguments().getLong(ARG_VEHICLE_ID);
        long availableUntil = viewModel.getAvailableUntil(binding.publishDay.getSelectedItemPosition(), binding.publishTime.getSelectedItemPosition());
        Result<Void> result = viewModel.publish(vehicleId, availableUntil);
        if (result instanceof Result.Success) {
            ((MyVehiclesFragment) requireParentFragment()).loadVehicles();
            dismiss();
        } else {
            showError(((Result.Error<Void>) result).code);
        }
    }

    private void showError(RepoError code) {
        int message = R.string.error_unexpected;
        if (code == VehicleError.NOT_APPROVED) {
            message = R.string.publish_vehicle_error_not_approved;
        } else if (code == VehicleError.DATE_NOT_FUTURE) {
            message = R.string.publish_vehicle_error_date;
        } else if (code == VehicleError.VEHICLE_DELETED) {
            message = R.string.publish_vehicle_error_deleted;
        } else if (code == VehicleError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == VehicleError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        }
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }
}
