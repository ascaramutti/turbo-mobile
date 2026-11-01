package com.turbo.mobile.ui.driver;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.TurboUi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** W-11 Trip photos: 1 to 5 camera photos of the vehicle, before starting (pickup) or ending (return) a trip. */
public class TripPhotosActivity extends AppCompatActivity {

    // Names of the Intent extras sent by DriverBookingDetailActivity.
    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_IS_RETURN = "is_return"; // false = pickup photos, true = return photos

    private static final int MAX_PHOTOS = 5;

    private AppContainer container;
    private long driverId;
    private long bookingId;
    private boolean isReturn;
    // ponytail: the photos live only in this screen, so rotating the phone clears them (save them in
    // onSaveInstanceState if that becomes a problem).
    private final List<Uri> photos = new ArrayList<>();
    private Uri cameraUri; // where the camera saves the photo being taken

    private ViewGroup photoGrid;
    private View addTile;
    private TextView progressCount;
    private TextView selectedChip;
    private Button finishButton;

    // Opens the camera app. It saves the photo in cameraUri and tells us if the user took it (true) or went back.
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), taken -> {
                if (taken) {
                    photos.add(cameraUri);
                    showPhotos();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_trip_photos);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();
        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        isReturn = getIntent().getBooleanExtra(EXTRA_IS_RETURN, false);

        TextView title = findViewById(R.id.photos_title);
        TextView subtitle = findViewById(R.id.photos_subtitle);
        photoGrid = findViewById(R.id.photo_grid);
        addTile = findViewById(R.id.add_photo);
        finishButton = findViewById(R.id.finish_photos);
        if (isReturn) {
            title.setText(R.string.return_photos);
            subtitle.setText(R.string.return_photos_subtitle);
            finishButton.setText(R.string.end_trip);
        } else {
            title.setText(R.string.pickup_photos);
            subtitle.setText(R.string.pickup_photos_subtitle);
            finishButton.setText(R.string.start_trip);
        }

        // The "Photo progress" card (item_info_card.xml, included in the layout).
        TextView cardTitle = findViewById(R.id.card_title);
        TextView footer = findViewById(R.id.card_footer);
        progressCount = findViewById(R.id.card_subtitle);
        selectedChip = findViewById(R.id.chip_primary);
        cardTitle.setText(R.string.photo_progress);
        footer.setText(R.string.tap_to_delete);
        footer.setTextColor(ContextCompat.getColor(this, R.color.turbo_error));
        findViewById(R.id.card_thumbnail).setVisibility(View.VISIBLE);

        addTile.setOnClickListener(v -> openCamera());
        finishButton.setOnClickListener(v -> sendPhotos());
        showPhotos();
    }

    private void openCamera() {
        try {
            cameraUri = container.imageStorage.createCameraUri(); // an empty file for the camera to fill
            takePhoto.launch(cameraUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    // One tile per photo (with its × to delete it), then "+ Add" while there is room for more.
    private void showPhotos() {
        photoGrid.removeAllViews();
        for (Uri photo : photos) {
            View tile = getLayoutInflater().inflate(R.layout.item_photo_tile, photoGrid, false);
            tile.setClipToOutline(true); // cuts the photo to the tile's rounded corners
            TurboUi.loadPhoto(this, tile.findViewById(R.id.photo_image), photo.toString());
            tile.findViewById(R.id.photo_remove).setOnClickListener(v -> {
                photos.remove(photo);
                showPhotos();
            });
            photoGrid.addView(tile);
        }
        if (photos.size() < MAX_PHOTOS) {
            photoGrid.addView(addTile);
        }

        // "3 of 5 photos" and "3 selected". The button needs at least one photo.
        progressCount.setText(getString(R.string.photo_progress_count, photos.size(), MAX_PHOTOS));
        TurboUi.paintChip(selectedChip, getString(R.string.photos_selected, photos.size()),
                R.color.turbo_primary_container, R.color.turbo_primary);
        finishButton.setEnabled(!photos.isEmpty());
    }

    // "Start trip" sends the pickup photos, "End trip" the return photos.
    private void sendPhotos() {
        List<Uri> chosen = new ArrayList<>(photos); // a copy for the background thread
        finishButton.setEnabled(false); // no double taps while sending
        container.executor.execute(() -> {
            Result<Void> result;
            if (isReturn) {
                result = container.bookingRepository.endTrip(driverId, bookingId, chosen);
            } else {
                result = container.bookingRepository.startTrip(driverId, bookingId, chosen);
            }
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    int message = R.string.trip_started;
                    if (isReturn) {
                        message = R.string.trip_ended;
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish(); // back to the booking, which reloads in onResume
                } else {
                    TurboUi.showError(this, result);
                    finishButton.setEnabled(true);
                }
            });
        });
    }
}
