package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.databinding.ActivityVehicleDetailBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.util.ImageStorage;

import java.util.List;
import java.util.Locale;

public class VehicleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private static final int[] DURATIONS = {4, 8};

    private ActivityVehicleDetailBinding binding;
    private BookingViewModel viewModel;
    private ImageStorage imageStorage;
    private VehicleDetail vehicle;
    private List<Long> shiftDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVehicleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new BookingViewModel(appContainer.vehicleRepository, appContainer.documentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        binding.vehiclePhoto.setClipToOutline(true);
        load();
    }

    private void load() {
        long vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);
        Result<VehicleDetail> vehicleResult = viewModel.loadVehicle(vehicleId);
        if (vehicleResult instanceof Result.Error) {
            RepoError code = ((Result.Error<VehicleDetail>) vehicleResult).code;
            showError(code == VehicleError.NOT_FOUND ? R.string.vehicle_detail_not_available : R.string.error_unexpected);
            return;
        }
        vehicle = ((Result.Success<VehicleDetail>) vehicleResult).data;

        Result<DriverStatus> statusResult = viewModel.loadDriverStatus();
        if (statusResult instanceof Result.Error) {
            RepoError code = ((Result.Error<DriverStatus>) statusResult).code;
            showError(code == DocumentError.NOT_FOUND ? R.string.error_not_found : R.string.error_unexpected);
            return;
        }
        DriverStatus status = ((Result.Success<DriverStatus>) statusResult).data;

        shiftDays = viewModel.getShiftDays(vehicle.getAvailableUntil());
        if (!vehicle.isPublished() || shiftDays.isEmpty()) {
            showError(R.string.vehicle_detail_not_available);
            return;
        }

        showVehicle();
        if (status.canBook()) {
            setUpShift(status);
        } else {
            binding.documentsBanner.setVisibility(View.VISIBLE);
            binding.shiftCard.setVisibility(View.GONE);
        }
    }

    private void showError(int message) {
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
        binding.content.setVisibility(View.GONE);
        binding.requestBooking.setVisibility(View.GONE);
    }

    private void showVehicle() {
        binding.vehicleTitle.setText(vehicle.getMake() + " " + vehicle.getModel() + " " + vehicle.getYear());
        binding.chipCategory.setText(getResources().getStringArray(R.array.vehicle_category_names)[vehicle.getCategory().ordinal()]);
        binding.chipFuel.setText(getResources().getStringArray(R.array.vehicle_fuel_names)[vehicle.getFuelType().ordinal()]);
        binding.chipService.setText(getResources().getStringArray(R.array.vehicle_service_names)[vehicle.getServiceType().ordinal()]);

        if (vehicle.getThumbnailPath() != null) {
            binding.vehiclePhoto.setImageBitmap(imageStorage.load(vehicle.getThumbnailPath(), 800));
        }

        binding.ownerName.setText(vehicle.getOwnerName());
        if (vehicle.getOwnerRatingAvg() == null) {
            binding.ownerRating.setText(R.string.vehicle_detail_no_reviews);
        } else {
            int count = vehicle.getOwnerRatingCount();
            binding.ownerRating.setText(getResources().getQuantityString(R.plurals.vehicle_detail_rating, count, vehicle.getOwnerRatingAvg(), count));
        }

        binding.pickupAddress.setText(vehicle.getAddress());
        binding.pickupZone.setText(vehicle.getZoneName());

        long until = vehicle.getAvailableUntil();
        binding.availableUntil.setText(getString(R.string.vehicle_detail_available_until, DisplayFormat.dayAndDate(until), DisplayFormat.time(until)));
    }

    private void setUpShift(DriverStatus status) {
        String[] days = new String[shiftDays.size()];
        for (int i = 0; i < shiftDays.size(); i++) {
            days[i] = DisplayFormat.dayAndDate(shiftDays.get(i));
        }
        ScreenSetup.fillSpinner(binding.shiftDay, days);
        if (shiftDays.size() > 1) {
            binding.shiftDay.setSelection(1);
        }

        String[] times = new String[48];
        for (int i = 0; i < times.length; i++) {
            times[i] = String.format(Locale.US, "%02d:%02d", i / 2, i % 2 * 30);
        }
        ScreenSetup.fillSpinner(binding.shiftTime, times);
        binding.shiftTime.setSelection(16);

        String[] durations = new String[DURATIONS.length];
        for (int i = 0; i < DURATIONS.length; i++) {
            durations[i] = getString(R.string.vehicle_detail_hours, DURATIONS[i]);
        }
        ScreenSetup.fillSpinner(binding.shiftDuration, durations);

        String[] services = getResources().getStringArray(R.array.vehicle_detail_services);
        boolean taxiAllowed = status.getLicenseClass() == LicenseClass.CLASS_4
                && vehicle.getServiceType() == VehicleServiceType.TAXI_AND_DELIVERY;
        if (!taxiAllowed) {
            services = new String[]{services[0]};
        }
        ScreenSetup.fillSpinner(binding.shiftService, services);

        AdapterView.OnItemSelectedListener shiftChanged = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showShift();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        binding.shiftDay.setOnItemSelectedListener(shiftChanged);
        binding.shiftTime.setOnItemSelectedListener(shiftChanged);
        binding.shiftDuration.setOnItemSelectedListener(shiftChanged);
    }

    private void showShift() {
        long day = shiftDays.get(binding.shiftDay.getSelectedItemPosition());
        long start = viewModel.getShiftStart(day, binding.shiftTime.getSelectedItemPosition());
        int hours = DURATIONS[binding.shiftDuration.getSelectedItemPosition()];
        int rate = vehicle.getHourlyRateCents();
        String total = DisplayFormat.money(viewModel.computeTotalCents(hours, rate));
        binding.shiftPrice.setText(getString(R.string.vehicle_detail_price, hours, DisplayFormat.money(rate), total));

        long until = vehicle.getAvailableUntil();
        boolean tooSoon = viewModel.startsTooSoon(start);
        boolean afterAvailable = viewModel.endsAfterAvailable(start, hours, until);
        if (tooSoon) {
            binding.shiftError.setText(R.string.vehicle_detail_too_soon);
            binding.shiftError.setVisibility(View.VISIBLE);
        } else if (afterAvailable) {
            binding.shiftError.setText(getString(R.string.vehicle_detail_after_available, DisplayFormat.dayAndDate(until), DisplayFormat.time(until)));
            binding.shiftError.setVisibility(View.VISIBLE);
        } else {
            binding.shiftError.setVisibility(View.GONE);
        }
        binding.requestBooking.setEnabled(!tooSoon && !afterAvailable);
    }
}
