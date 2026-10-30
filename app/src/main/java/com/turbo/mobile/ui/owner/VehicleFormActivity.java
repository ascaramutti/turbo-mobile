package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.FuelType;
import com.turbo.mobile.data.model.PhotoItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.data.model.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** W-15 Vehicle form. Opens as "Add vehicle", or as "Edit ..." when it receives a vehicle id. */
public class VehicleFormActivity extends AppCompatActivity {

    // Name of the Intent extra with the id of the vehicle to edit (see MyVehiclesFragment).
    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private AppContainer container;
    private long ownerId;
    private long vehicleId; // 0 when adding a new vehicle

    // The vehicle being edited. In Edit it comes from the repository, so the fields this screen
    // doesn't show (latitude, longitude) keep their values when we save.
    private VehicleForm form = new VehicleForm();
    private final List<PhotoItem> photos = new ArrayList<>();
    private List<Zone> zones = new ArrayList<>();
    private Uri insurance;
    private Uri registration;

    private ScrollView scroll;
    private TextView title;
    private LinearLayout photoList;
    private TextView addPhotoButton;
    private View photoMinimum;
    private View errorBanner;
    private EditText makeInput;
    private EditText modelInput;
    private EditText yearInput;
    private EditText plateInput;
    private EditText vinInput;
    private EditText rateInput;
    private EditText descriptionInput;
    private Spinner categorySpinner;
    private Spinner fuelSpinner;
    private Spinner serviceSpinner;
    private Spinner zoneSpinner;
    private TextView insuranceButton;
    private TextView registrationButton;
    private TextView locationButton;
    private Button saveButton;

    // Each launcher opens the phone's gallery ("image/*" = any image). When the user picks one,
    // Android runs the code after the arrow with the image's Uri (null if they went back without picking).
    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    addPhoto(uri);
                }
            });

    private final ActivityResultLauncher<String> pickInsurance =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    insurance = uri;
                    insuranceButton.setText(R.string.photo_selected);
                }
            });

    private final ActivityResultLauncher<String> pickRegistration =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    registration = uri;
                    registrationButton.setText(R.string.photo_selected);
                }
            });

    // Opens our own W-17 Vehicle location screen. When it closes with "Confirm location" (RESULT_OK),
    // its Intent brings the address back.
    private final ActivityResultLauncher<Intent> pickLocation =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    form.setAddress(result.getData().getStringExtra(VehicleLocationActivity.EXTRA_ADDRESS));
                    locationButton.setText(form.getAddress());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OwnerUi.setUpScreen(this, R.layout.activity_vehicle_form);
        OwnerUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);

        scroll = findViewById(R.id.form_scroll);
        title = findViewById(R.id.form_title);
        photoList = findViewById(R.id.photo_list);
        addPhotoButton = findViewById(R.id.add_photo);
        photoMinimum = findViewById(R.id.photo_minimum);
        errorBanner = findViewById(R.id.error_banner);
        makeInput = findViewById(R.id.input_make);
        modelInput = findViewById(R.id.input_model);
        yearInput = findViewById(R.id.input_year);
        plateInput = findViewById(R.id.input_plate);
        vinInput = findViewById(R.id.input_vin);
        rateInput = findViewById(R.id.input_rate);
        descriptionInput = findViewById(R.id.input_description);
        categorySpinner = findViewById(R.id.spinner_category);
        fuelSpinner = findViewById(R.id.spinner_fuel_type);
        serviceSpinner = findViewById(R.id.spinner_service);
        zoneSpinner = findViewById(R.id.spinner_zone);
        insuranceButton = findViewById(R.id.pick_insurance);
        registrationButton = findViewById(R.id.pick_registration);
        locationButton = findViewById(R.id.pick_location);
        saveButton = findViewById(R.id.save);

        setUpSpinner(categorySpinner, R.array.vehicle_categories);
        setUpSpinner(fuelSpinner, R.array.fuel_types);
        setUpSpinner(serviceSpinner, R.array.service_types);

        addPhotoButton.setOnClickListener(v -> pickPhoto.launch("image/*"));
        insuranceButton.setOnClickListener(v -> pickInsurance.launch("image/*"));
        registrationButton.setOnClickListener(v -> pickRegistration.launch("image/*"));
        locationButton.setOnClickListener(v -> {
            // Opens W-17 with the current address; it sends the new one back to pickLocation.
            Intent intent = new Intent(this, VehicleLocationActivity.class);
            intent.putExtra(VehicleLocationActivity.EXTRA_ADDRESS, form.getAddress());
            pickLocation.launch(intent);
        });
        saveButton.setOnClickListener(v -> save());

        if (vehicleId != 0) {
            // Edit. The title is set once the vehicle is loaded (fillForm).
            findViewById(R.id.warning_banner).setVisibility(View.VISIBLE);
            findViewById(R.id.documents).setVisibility(View.GONE); // documents are changed in W-16
            vinInput.setEnabled(false); // the VIN can't change after the vehicle is registered
            saveButton.setText(R.string.save_changes);
        }
        showPhotos();
        loadZones();
    }

    // Fills a dropdown with a list from strings_owner.xml. The first option is always "Select".
    private void setUpSpinner(Spinner spinner, int options) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, options, R.layout.item_select);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // The zones come from the repository, so their dropdown is filled here instead of from strings_owner.xml.
    private void loadZones() {
        container.executor.execute(() -> {
            List<Zone> zoneList = container.vehicleRepository.getZones();
            runOnUiThread(() -> {
                zones = zoneList;
                List<String> names = new ArrayList<>();
                names.add(getString(R.string.select));
                for (Zone zone : zones) {
                    names.add(zone.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_select, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                zoneSpinner.setAdapter(adapter);
                if (vehicleId != 0) {
                    loadVehicle(); // after the zones, so the vehicle's zone can be selected
                }
            });
        });
    }

    private void loadVehicle() {
        container.executor.execute(() -> {
            Result<VehicleEditData> result = container.vehicleRepository.getVehicleForEdit(ownerId, vehicleId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    fillForm(((Result.Success<VehicleEditData>) result).data);
                } else {
                    OwnerUi.showError(this, result);
                    finish();
                }
            });
        });
    }

    // Edit: puts the saved vehicle in the inputs.
    private void fillForm(VehicleEditData data) {
        form = data.getForm();
        title.setText(getString(R.string.edit_vehicle_title, form.getMake() + " " + form.getModel()));
        makeInput.setText(form.getMake());
        modelInput.setText(form.getModel());
        yearInput.setText(String.valueOf(form.getYear()));
        plateInput.setText(form.getPlate());
        vinInput.setText(form.getVin());
        // ordinal() is the value's position in its enum (SEDAN = 0, SUV = 1...). +1 because 0 is "Select".
        categorySpinner.setSelection(form.getCategory().ordinal() + 1);
        fuelSpinner.setSelection(form.getFuelType().ordinal() + 1);
        serviceSpinner.setSelection(form.getServiceType().ordinal() + 1);
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getId() == form.getZoneId()) {
                zoneSpinner.setSelection(i + 1);
            }
        }
        // The repository stores the rate in cents: 1250 -> "12.50".
        rateInput.setText(String.format(Locale.US, "%.2f", form.getHourlyRateCents() / 100.0));
        descriptionInput.setText(form.getDescription());
        locationButton.setText(form.getAddress());
        photos.addAll(data.getPhotos());
        showPhotos();
    }

    // One tile per photo, then "+ Add photo". The "Minimum 1" tile only shows while there are no photos.
    private void showPhotos() {
        photoList.removeAllViews();
        for (PhotoItem photo : photos) {
            View tile = getLayoutInflater().inflate(R.layout.item_photo_tile, photoList, false);
            tile.setClipToOutline(true); // cuts the photo to the tile's rounded corners
            ImageView image = tile.findViewById(R.id.photo_image);
            loadPhoto(image, photo.getFilePath());
            tile.findViewById(R.id.photo_remove).setOnClickListener(v -> removePhoto(photo));
            photoList.addView(tile);
        }
        if (photos.isEmpty()) {
            addPhotoButton.setText(R.string.add_photo);
            photoMinimum.setVisibility(View.VISIBLE);
        } else {
            addPhotoButton.setText(R.string.add_photo_short);
            photoMinimum.setVisibility(View.GONE);
        }
    }

    // Reading an image file is slow, so it also happens on the background thread.
    private void loadPhoto(ImageView image, String path) {
        container.executor.execute(() -> {
            Bitmap bitmap = container.imageStorage.load(path, 300);
            if (bitmap != null) {
                runOnUiThread(() -> image.setImageBitmap(bitmap));
            }
        });
    }

    private void addPhoto(Uri uri) {
        PhotoItem photo = new PhotoItem();
        photo.setFilePath(uri.toString());
        if (vehicleId == 0) {
            // Add: keep it in the list. It's saved together with the vehicle in save().
            photos.add(photo);
            showPhotos();
        } else {
            // Edit: save it right away.
            container.executor.execute(() -> {
                Result<Long> result = container.vehicleRepository.addVehiclePhoto(ownerId, vehicleId, uri);
                runOnUiThread(() -> {
                    if (result instanceof Result.Success) {
                        photo.setId(((Result.Success<Long>) result).data); // needed to delete it later
                        photos.add(photo);
                        showPhotos();
                    } else {
                        OwnerUi.showError(this, result);
                    }
                });
            });
        }
    }

    private void removePhoto(PhotoItem photo) {
        if (vehicleId == 0) {
            photos.remove(photo);
            showPhotos();
        } else {
            container.executor.execute(() -> {
                Result<Void> result = container.vehicleRepository.deleteVehiclePhoto(ownerId, photo.getId());
                runOnUiThread(() -> {
                    if (result instanceof Result.Success) {
                        photos.remove(photo);
                        showPhotos();
                    } else {
                        OwnerUi.showError(this, result);
                    }
                });
            });
        }
    }

    private void save() {
        if (!readForm()) {
            errorBanner.setVisibility(View.VISIBLE);
            scroll.smoothScrollTo(0, 0); // scroll up so the user sees the message
            return;
        }
        errorBanner.setVisibility(View.GONE);
        saveButton.setEnabled(false); // no double taps while saving

        List<Uri> photoUris = new ArrayList<>();
        for (PhotoItem photo : photos) {
            photoUris.add(Uri.parse(photo.getFilePath()));
        }
        container.executor.execute(() -> {
            // Result<?> because registerVehicle returns Result<Long> and updateVehicle Result<Void>.
            Result<?> result;
            if (vehicleId == 0) {
                result = container.vehicleRepository.registerVehicle(ownerId, form, photoUris, insurance, registration);
            } else {
                result = container.vehicleRepository.updateVehicle(ownerId, vehicleId, form);
            }
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    finish(); // back to My vehicles, which reloads its list in onResume
                } else {
                    OwnerUi.showError(this, result);
                    saveButton.setEnabled(true);
                }
            });
        });
    }

    // Copies the inputs into `form`. Returns false if something required is missing.
    private boolean readForm() {
        String make = makeInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        String year = yearInput.getText().toString().trim();
        String plate = plateInput.getText().toString().trim();
        String vin = vinInput.getText().toString().trim();
        String rate = rateInput.getText().toString().trim();
        // Position 0 of every dropdown is "Select", so 0 means nothing was chosen
        // (and the zones dropdown gives -1 while the zones are still loading).
        int category = categorySpinner.getSelectedItemPosition();
        int fuelType = fuelSpinner.getSelectedItemPosition();
        int service = serviceSpinner.getSelectedItemPosition();
        int zone = zoneSpinner.getSelectedItemPosition();

        if (make.isEmpty() || model.isEmpty() || year.isEmpty() || plate.isEmpty() || vin.isEmpty()
                || rate.isEmpty() || category == 0 || fuelType == 0 || service == 0 || zone <= 0
                || form.getAddress() == null || photos.isEmpty()) {
            return false;
        }
        if (vehicleId == 0 && (insurance == null || registration == null)) {
            return false; // a new vehicle needs both documents
        }
        double rateCad;
        try {
            rateCad = Double.parseDouble(rate);
        } catch (NumberFormatException e) {
            return false; // for example just "."
        }
        if (rateCad <= 0) {
            return false;
        }

        form.setMake(make);
        form.setModel(model);
        form.setYear(Integer.parseInt(year));
        form.setPlate(plate);
        form.setVin(vin);
        // -1 because the dropdowns have "Select" before the enum values.
        form.setCategory(VehicleCategory.values()[category - 1]);
        form.setFuelType(FuelType.values()[fuelType - 1]);
        form.setServiceType(VehicleServiceType.values()[service - 1]);
        form.setZoneId(zones.get(zone - 1).getId());
        form.setHourlyRateCents((int) Math.round(rateCad * 100)); // 12.50 -> 1250 cents
        form.setDescription(descriptionInput.getText().toString().trim());
        return true;
    }
}
