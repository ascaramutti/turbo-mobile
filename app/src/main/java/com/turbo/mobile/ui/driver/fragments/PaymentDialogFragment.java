package com.turbo.mobile.ui.driver.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.DialogPaymentBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.driver.BookingDetailActivity;
import com.turbo.mobile.ui.driver.viewmodels.TripViewModel;

public class PaymentDialogFragment extends DialogFragment {

    public static final String ARG_BOOKING_ID = "booking_id";
    public static final String ARG_TOTAL = "total";

    private DialogPaymentBinding binding;
    private TripViewModel viewModel;

    public PaymentDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogPaymentBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new TripViewModel(appContainer.paymentRepository, appContainer.sessionManager);

        String total = DisplayFormat.money(requireArguments().getInt(ARG_TOTAL));
        binding.amount.setText(getString(R.string.payment_amount, total));
        binding.pay.setText(getString(R.string.payment_pay, total));

        binding.cancel.setOnClickListener(v -> dismiss());
        binding.pay.setOnClickListener(v -> pay());

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

    private void pay() {
        binding.errorBanner.setVisibility(View.GONE);

        String number = binding.cardNumber.getText().toString().replace(" ", "");
        String expiry = binding.expiry.getText().toString().trim();
        String cvv = binding.cvv.getText().toString().trim();

        if (!viewModel.isCardNumberValid(number)) {
            showMessage(R.string.payment_error_card);
            return;
        }
        if (!viewModel.isExpiryValid(expiry)) {
            showMessage(R.string.payment_error_expiry);
            return;
        }
        if (!viewModel.isCvvValid(cvv)) {
            showMessage(R.string.payment_error_cvv);
            return;
        }

        String cardLast4 = number.substring(12);
        Result<Void> result = viewModel.pay(requireArguments().getLong(ARG_BOOKING_ID), cardLast4);
        if (result instanceof Result.Success) {
            ((BookingDetailActivity) requireActivity()).paymentDone();
            dismiss();
        } else {
            showError(((Result.Error<Void>) result).code);
        }
    }

    private void showError(RepoError code) {
        int message = R.string.error_unexpected;
        if (code == PaymentError.NOT_CONFIRMED) {
            message = R.string.payment_error_not_confirmed;
        } else if (code == PaymentError.ALREADY_PAID) {
            message = R.string.payment_error_already_paid;
        } else if (code == PaymentError.PAYMENT_DEADLINE_PASSED) {
            message = R.string.payment_error_deadline;
        } else if (code == PaymentError.INVALID_CARD) {
            message = R.string.payment_error_card;
        } else if (code == PaymentError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == PaymentError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        }
        showMessage(message);
    }

    private void showMessage(int message) {
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }
}
