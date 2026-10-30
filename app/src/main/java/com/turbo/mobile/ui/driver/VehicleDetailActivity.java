package com.turbo.mobile.ui.driver;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.ui.TurboUi;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** W-06 Vehicle detail + Book a shift, and W-07 the "Request this booking?" confirmation. */
public class VehicleDetailActivity extends AppCompatActivity {

    // Name of the Intent extra with the vehicle's id (sent by SearchFragment).
    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private static final long HOUR = 60 * 60 * 1000; // one hour in milliseconds

    private AppContainer container;
    private long driverId;
    private VehicleDetail vehicle;
    private WeeklyHours weeklyHours;
    private boolean canBook; // false while the driver's documents are not both approved
    private int photoIndex = 0; // position of the photo shown in the gallery

    // The shift being booked. It starts as tomorrow at 08:00, 4 hours, delivery.
    private final Calendar start = Calendar.getInstance();
    private int hours = 4;
    private ServiceType service = ServiceType.DELIVERY;

    private TextView title;
    private TextView categoryChip;
    private TextView fuelChip;
    private TextView serviceChip;
    private ImageView galleryImage;
    private TextView galleryCounter;
    private View limitSection;
    private TextView weekHours;
    private TextView selectedHours;
    private TextView limitBanner;
    private View bookingSection;
    private TextView ownerName;
    private TextView ownerRating;
    private TextView pickupAddress;
    private TextView pickupZone;
    private TextView shiftStart;
    private TextView fourHours;
    private TextView eightHours;
    private TextView taxiChip;
    private TextView deliveryChip;
    private TextView shiftPrice;
    private View documentsSection;
    private LinearLayout documentList;
    private Button reviewDocumentsButton;
    private Button requestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_vehicle_detail);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();

        title = findViewById(R.id.vehicle_title);
        categoryChip = findViewById(R.id.chip_category);
        fuelChip = findViewById(R.id.chip_fuel);
        serviceChip = findViewById(R.id.chip_service);
        galleryImage = findViewById(R.id.gallery_image);
        galleryCounter = findViewById(R.id.gallery_counter);
        limitSection = findViewById(R.id.limit_section);
        weekHours = findViewById(R.id.week_hours);
        selectedHours = findViewById(R.id.selected_hours);
        limitBanner = findViewById(R.id.limit_banner);
        bookingSection = findViewById(R.id.booking_section);
        ownerName = findViewById(R.id.owner_name);
        ownerRating = findViewById(R.id.owner_rating);
        pickupAddress = findViewById(R.id.pickup_address);
        pickupZone = findViewById(R.id.pickup_zone);
        shiftStart = findViewById(R.id.shift_start);
        fourHours = findViewById(R.id.hours_4);
        eightHours = findViewById(R.id.hours_8);
        taxiChip = findViewById(R.id.service_taxi);
        deliveryChip = findViewById(R.id.service_delivery);
        shiftPrice = findViewById(R.id.shift_price);
        documentsSection = findViewById(R.id.documents_section);
        documentList = findViewById(R.id.document_list);
        reviewDocumentsButton = findViewById(R.id.review_documents);
        requestButton = findViewById(R.id.request_booking);

        // Tomorrow at 08:00:00.
        start.add(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 8);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        findViewById(R.id.gallery).setClipToOutline(true); // cuts the photo to the rounded corners
        findViewById(R.id.gallery).setOnClickListener(v -> nextPhoto());
        shiftStart.setOnClickListener(v -> pickDay());
        fourHours.setOnClickListener(v -> {
            hours = 4;
            showShift();
        });
        eightHours.setOnClickListener(v -> {
            hours = 8;
            showShift();
        });
        taxiChip.setOnClickListener(v -> {
            service = ServiceType.TAXI;
            showShift();
        });
        deliveryChip.setOnClickListener(v -> {
            service = ServiceType.DELIVERY;
            showShift();
        });
        reviewDocumentsButton.setOnClickListener(v -> startActivity(new Intent(this, MyDocumentsActivity.class)));
        requestButton.setOnClickListener(v -> confirmRequest());
    }

    // Load in onResume, so coming back from "Review my documents" shows the documents' new status.
    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    // Reads the vehicle, whether the driver may book, the driver's hours this week and their documents.
    private void load() {
        long vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);
        container.executor.execute(() -> {
            Result<VehicleDetail> vehicleResult = container.vehicleRepository.getVehicleDetail(vehicleId);
            Result<DriverStatus> statusResult = container.documentRepository.getDriverStatus(driverId);
            Result<WeeklyHours> hoursResult = container.bookingRepository.getWeeklyHours(driverId);
            List<DriverDocumentItem> documents = container.documentRepository.getDriverDocuments(driverId);
            runOnUiThread(() -> {
                if (failed(vehicleResult) || failed(statusResult) || failed(hoursResult)) {
                    return;
                }
                vehicle = ((Result.Success<VehicleDetail>) vehicleResult).data;
                weeklyHours = ((Result.Success<WeeklyHours>) hoursResult).data;
                canBook = ((Result.Success<DriverStatus>) statusResult).data.canBook();
                showVehicle();
                showDocuments(documents);
                showShift();
            });
        });
    }

    // If the result is an error: shows its message, closes the screen and returns true.
    private boolean failed(Result<?> result) {
        if (result instanceof Result.Error) {
            TurboUi.showError(this, result);
            finish();
            return true;
        }
        return false;
    }

    private void showVehicle() {
        title.setText(vehicle.getMake() + " " + vehicle.getModel() + " " + vehicle.getYear());
        categoryChip.setText(TurboUi.categoryName(this, vehicle.getCategory()));
        categoryChip.setSelected(true); // the category is the orange chip, as in the wireframe
        // The fuel list in strings_owner.xml starts with "Select", so the enum's position is +1.
        fuelChip.setText(getResources().getStringArray(R.array.fuel_types)[vehicle.getFuelType().ordinal() + 1]);
        serviceChip.setText(TurboUi.serviceName(this, vehicle.getServiceType()));

        ownerName.setText(vehicle.getOwnerName());
        if (vehicle.getOwnerRatingAvg() == null) {
            ownerRating.setText(R.string.no_reviews_yet);
        } else {
            // "★ 4.8 · 12 reviews"
            int count = vehicle.getOwnerRatingCount();
            ownerRating.setText("★ " + String.format(Locale.US, "%.1f", vehicle.getOwnerRatingAvg()) + " · "
                    + getResources().getQuantityString(R.plurals.review_count, count, count));
        }
        pickupAddress.setText(vehicle.getAddress());
        pickupZone.setText(vehicle.getZoneName());

        // "Delivery only" vehicles can't be booked for taxi.
        if (vehicle.getServiceType() == VehicleServiceType.DELIVERY_ONLY) {
            taxiChip.setVisibility(View.GONE);
            service = ServiceType.DELIVERY;
        }
        showPhoto();
    }

    // Shows photo number photoIndex and "1 of 4" under it.
    private void showPhoto() {
        List<String> photos = vehicle.getPhotoPaths();
        if (photos.isEmpty()) {
            return; // the gallery keeps its icon and "Vehicle gallery"
        }
        TurboUi.loadPhoto(this, galleryImage, photos.get(photoIndex));
        galleryCounter.setText(getString(R.string.photo_counter, photoIndex + 1, photos.size()));
        galleryCounter.setVisibility(View.VISIBLE);
    }

    // Tapping the gallery shows the next photo. After the last one it goes back to the first.
    private void nextPhoto() {
        if (vehicle == null || vehicle.getPhotoPaths().isEmpty()) {
            return;
        }
        photoIndex = photoIndex + 1;
        if (photoIndex == vehicle.getPhotoPaths().size()) {
            photoIndex = 0;
        }
        showPhoto();
    }

    // "Documents required" version: while the driver can't book, their documents replace the booking part.
    private void showDocuments(List<DriverDocumentItem> documents) {
        if (canBook) {
            bookingSection.setVisibility(View.VISIBLE);
            documentsSection.setVisibility(View.GONE);
            reviewDocumentsButton.setVisibility(View.GONE);
            return;
        }
        bookingSection.setVisibility(View.GONE);
        documentsSection.setVisibility(View.VISIBLE);
        reviewDocumentsButton.setVisibility(View.VISIBLE);
        documentList.removeAllViews();
        for (DriverDocumentItem document : documents) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, documentList, false);
            MyDocumentsActivity.fillCard(this, card, document); // the same card as in W-13
            documentList.addView(card);
        }
    }

    // Shows the chosen shift and its price, and checks the weekly limit. Runs after every change.
    private void showShift() {
        if (vehicle == null) {
            return; // a chip was tapped before the vehicle finished loading
        }
        long startMillis = start.getTimeInMillis();
        shiftStart.setText(TurboUi.dayAndDate(startMillis) + " · " + TurboUi.time(startMillis));
        fourHours.setSelected(hours == 4);
        eightHours.setSelected(hours == 8);
        taxiChip.setSelected(service == ServiceType.TAXI);
        deliveryChip.setSelected(service == ServiceType.DELIVERY);

        // "4 h × CAD 12.50 · Total CAD 50.00"
        int rate = vehicle.getHourlyRateCents();
        shiftPrice.setText(getString(R.string.shift_price, hours, TurboUi.money(rate), TurboUi.money(hours * rate)));

        // A driver can book up to 24 hours a week. Only a shift in the current week adds to this week's hours.
        boolean thisWeek = startMillis < weeklyHours.getWeekEnd();
        boolean overLimit = thisWeek && weeklyHours.getBookedHours() + hours > weeklyHours.getLimitHours();
        if (overLimit) {
            int limit = weeklyHours.getLimitHours();
            weekHours.setText(getString(R.string.hours_of_limit, (int) weeklyHours.getBookedHours(), limit));
            selectedHours.setText(getString(R.string.hours_short, hours));
            limitBanner.setText(getString(R.string.weekly_limit_exceeded, hours, limit));
            limitSection.setVisibility(View.VISIBLE);
        } else {
            limitSection.setVisibility(View.GONE);
        }
        requestButton.setEnabled(canBook && !overLimit);
    }

    // First Android's calendar to choose the day, then its clock to choose the time.
    private void pickDay() {
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            start.set(year, month, day);
            pickTime();
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(System.currentTimeMillis()); // past days can't be picked
        picker.show();
    }

    private void pickTime() {
        TimePickerDialog picker = new TimePickerDialog(this, (view, hour, minute) -> {
            // Shifts start on the hour or at half past, so 08:10 becomes 08:00 and 08:40 becomes 08:30.
            int roundedMinute = 0;
            if (minute >= 30) {
                roundedMinute = 30;
            }
            start.set(Calendar.HOUR_OF_DAY, hour);
            start.set(Calendar.MINUTE, roundedMinute);
            showShift();
        }, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), true); // true = 24-hour clock
        picker.show();
    }

    // W-07: a summary of the shift before sending the request.
    private void confirmRequest() {
        long startMillis = start.getTimeInMillis();
        long endMillis = startMillis + hours * HOUR;
        String serviceName = getString(R.string.service_delivery);
        if (service == ServiceType.TAXI) {
            serviceName = getString(R.string.service_taxi);
        }
        // "Toyota Corolla · Delivery / Wed, Oct 14 · 08:00–12:00 · 4 hours / Total CAD 50.00 / You'll pay..."
        String message = getString(R.string.request_summary, vehicle.getMake() + " " + vehicle.getModel(),
                serviceName, TurboUi.dayAndDate(startMillis), TurboUi.timeRange(startMillis, endMillis), hours,
                TurboUi.money(hours * vehicle.getHourlyRateCents()));
        TurboUi.showConfirmDialog(this, getString(R.string.request_title), message,
                getString(R.string.cancel), getString(R.string.send_request), () -> sendRequest(startMillis, endMillis));
    }

    private void sendRequest(long startMillis, long endMillis) {
        ServiceType chosenService = service;
        requestButton.setEnabled(false); // no double taps while sending
        container.executor.execute(() -> {
            Result<Long> result = container.bookingRepository.createBooking(driverId, vehicle.getId(),
                    startMillis, endMillis, chosenService);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.booking_request_sent, Toast.LENGTH_SHORT).show();
                    finish(); // back to the search; the request is in My bookings
                } else {
                    TurboUi.showError(this, result); // e.g. the driver already has a booking at that time
                    requestButton.setEnabled(true);
                }
            });
        });
    }
}
