package com.turbo.mobile.ui.driver.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.databinding.DialogBookingConfirmBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.driver.VehicleDetailActivity;
import com.turbo.mobile.ui.driver.viewmodels.BookingViewModel;

public class BookingConfirmDialogFragment extends DialogFragment {

    public static final String ARG_VEHICLE_ID = "vehicle_id";
    public static final String ARG_VEHICLE_TITLE = "vehicle_title";
    public static final String ARG_HOURLY_RATE = "hourly_rate";
    public static final String ARG_AVAILABLE_UNTIL = "available_until";
    public static final String ARG_START = "start";
    public static final String ARG_HOURS = "hours";
    public static final String ARG_SERVICE_POSITION = "service_position";

    private static final ServiceType[] SERVICES = {ServiceType.DELIVERY, ServiceType.TAXI};

    private DialogBookingConfirmBinding binding;
    private BookingViewModel viewModel;

    public BookingConfirmDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogBookingConfirmBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new BookingViewModel(appContainer.vehicleRepository, appContainer.documentRepository, appContainer.bookingRepository, appContainer.sessionManager);

        showSummary();

        binding.cancel.setOnClickListener(v -> dismiss());
        binding.request.setOnClickListener(v -> request());

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

    private void showSummary() {
        Bundle arguments = requireArguments();
        long start = arguments.getLong(ARG_START);
        int hours = arguments.getInt(ARG_HOURS);
        long end = viewModel.getShiftEnd(start, hours);
        long deadline = viewModel.getPayDeadline(start);
        int total = viewModel.computeTotalCents(hours, arguments.getInt(ARG_HOURLY_RATE));
        String service = getResources().getStringArray(R.array.vehicle_detail_services)[arguments.getInt(ARG_SERVICE_POSITION)];

        binding.vehicleSummary.setText(getString(R.string.booking_confirm_vehicle, arguments.getString(ARG_VEHICLE_TITLE), service));
        binding.shiftSummary.setText(getString(R.string.booking_confirm_shift, DisplayFormat.dayAndDate(start), DisplayFormat.timeRange(start, end), hours));
        binding.total.setText(getString(R.string.booking_confirm_total, DisplayFormat.money(total)));
        binding.paymentNote.setText(getString(R.string.booking_confirm_payment, DisplayFormat.dayAndDate(deadline), DisplayFormat.time(deadline)));
    }

    private void request() {
        binding.errorBanner.setVisibility(View.GONE);

        Bundle arguments = requireArguments();
        long start = arguments.getLong(ARG_START);
        long end = viewModel.getShiftEnd(start, arguments.getInt(ARG_HOURS));
        ServiceType service = SERVICES[arguments.getInt(ARG_SERVICE_POSITION)];

        Result<Long> result = viewModel.createBooking(arguments.getLong(ARG_VEHICLE_ID), start, end, service);
        if (result instanceof Result.Success) {
            ((VehicleDetailActivity) requireActivity()).showRequestSent();
            dismiss();
        } else {
            showError(((Result.Error<Long>) result).code);
        }
    }

    private void showError(RepoError code) {
        binding.errorBanner.setText(errorMessage(code));
        binding.errorBanner.setVisibility(View.VISIBLE);
    }

    private String errorMessage(RepoError code) {
        if (code == BookingError.AFTER_AVAILABLE_UNTIL) {
            long until = requireArguments().getLong(ARG_AVAILABLE_UNTIL);
            return getString(R.string.vehicle_detail_after_available, DisplayFormat.dayAndDate(until), DisplayFormat.time(until));
        }

        int message = R.string.error_unexpected;
        if (code == BookingError.DRIVER_NOT_VERIFIED) {
            message = R.string.booking_confirm_error_not_verified;
        } else if (code == BookingError.DRIVER_INACTIVE) {
            message = R.string.booking_confirm_error_inactive;
        } else if (code == BookingError.DOCUMENTS_EXPIRE) {
            message = R.string.booking_confirm_error_documents_expire;
        } else if (code == BookingError.VEHICLE_NOT_PUBLISHED) {
            message = R.string.booking_confirm_error_not_published;
        } else if (code == BookingError.START_TOO_SOON) {
            message = R.string.vehicle_detail_too_soon;
        } else if (code == BookingError.START_TOO_FAR) {
            message = R.string.booking_confirm_error_too_far;
        } else if (code == BookingError.INVALID_START_STEP) {
            message = R.string.booking_confirm_error_start_step;
        } else if (code == BookingError.INVALID_DURATION) {
            message = R.string.booking_confirm_error_duration;
        } else if (code == BookingError.TAXI_NEEDS_CLASS_4) {
            message = R.string.booking_confirm_error_taxi_class;
        } else if (code == BookingError.TAXI_NOT_OFFERED) {
            message = R.string.booking_confirm_error_taxi_offered;
        } else if (code == BookingError.VEHICLE_OVERLAP) {
            message = R.string.booking_confirm_error_vehicle_overlap;
        } else if (code == BookingError.DRIVER_OVERLAP) {
            message = R.string.booking_confirm_error_driver_overlap;
        } else if (code == BookingError.WEEKLY_LIMIT) {
            message = R.string.booking_confirm_error_weekly_limit;
        } else if (code == BookingError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == BookingError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        }
        return getString(message);
    }
}
