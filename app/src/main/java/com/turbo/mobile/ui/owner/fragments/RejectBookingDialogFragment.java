package com.turbo.mobile.ui.owner.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.DialogRejectBookingBinding;
import com.turbo.mobile.ui.driver.BookingDetailActivity;
import com.turbo.mobile.ui.owner.viewmodels.OwnerBookingViewModel;

public class RejectBookingDialogFragment extends DialogFragment {

    public static final String ARG_BOOKING_ID = "booking_id";

    private DialogRejectBookingBinding binding;
    private OwnerBookingViewModel viewModel;

    public RejectBookingDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogRejectBookingBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new OwnerBookingViewModel(appContainer.bookingRepository, appContainer.sessionManager);

        binding.cancel.setOnClickListener(view -> dismiss());
        binding.reject.setOnClickListener(view -> reject());

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

    private void reject() {
        binding.errorBanner.setVisibility(View.GONE);
        String reason = binding.reason.getText().toString().trim();
        Result<Void> result = viewModel.reject(requireArguments().getLong(ARG_BOOKING_ID), reason);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        if (getParentFragment() instanceof BookingRequestsFragment) {
            ((BookingRequestsFragment) getParentFragment()).bookingRejected();
        } else {
            ((BookingDetailActivity) requireActivity()).bookingRejected();
        }
        dismiss();
    }
}
