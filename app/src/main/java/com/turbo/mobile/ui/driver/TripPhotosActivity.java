package com.turbo.mobile.ui.driver;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.ActivityTripPhotosBinding;
import com.turbo.mobile.databinding.ItemPhotoTileBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.viewmodels.TripViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TripPhotosActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_IS_RETURN = "is_return";

    private static final int MAX_PHOTOS = 5;
    private static final int PHOTO_SIZE = 300;

    private ActivityTripPhotosBinding binding;
    private TripViewModel viewModel;
    private ImageStorage imageStorage;
    private long bookingId;
    private boolean isReturn;
    private final List<Uri> photos = new ArrayList<>();
    private Uri cameraUri;

    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), photoTaken -> {
                if (photoTaken) {
                    photos.add(cameraUri);
                    showPhotos();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(view -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new TripViewModel(appContainer.bookingRepository, appContainer.paymentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        isReturn = getIntent().getBooleanExtra(EXTRA_IS_RETURN, false);
        if (isReturn) {
            binding.photosTitle.setText(R.string.trip_photos_return_title);
            binding.photosBody.setText(R.string.trip_photos_return_body);
            binding.sendPhotos.setText(R.string.trip_photos_end);
        } else {
            binding.photosTitle.setText(R.string.trip_photos_pickup_title);
            binding.photosBody.setText(R.string.trip_photos_pickup_body);
            binding.sendPhotos.setText(R.string.trip_photos_start);
        }

        binding.addPhoto.setOnClickListener(view -> openCamera());
        binding.sendPhotos.setOnClickListener(view -> sendPhotos());
        showPhotos();
    }

    private void openCamera() {
        binding.errorBanner.setVisibility(View.GONE);
        try {
            cameraUri = imageStorage.createCameraUri();
            takePhoto.launch(cameraUri);
        } catch (IOException exception) {
            showMessage(R.string.trip_photos_error_camera);
        }
    }

    private void showPhotos() {
        binding.photoList.removeAllViews();
        for (Uri photo : photos) {
            ItemPhotoTileBinding tile = ItemPhotoTileBinding.inflate(getLayoutInflater(), binding.photoList, false);
            tile.getRoot().setClipToOutline(true);
            tile.photoImage.setImageBitmap(imageStorage.load(photo.toString(), PHOTO_SIZE));
            tile.photoRemove.setOnClickListener(view -> removePhoto(photo));
            binding.photoList.addView(tile.getRoot());
        }
        binding.addPhoto.setVisibility(photos.size() < MAX_PHOTOS ? View.VISIBLE : View.GONE);
        binding.photoCount.setText(getString(R.string.trip_photos_count, photos.size(), MAX_PHOTOS));
    }

    private void removePhoto(Uri photo) {
        photos.remove(photo);
        showPhotos();
    }

    private void sendPhotos() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<Void> result;
        if (isReturn) {
            result = viewModel.endTrip(bookingId, photos);
        } else {
            result = viewModel.startTrip(bookingId, photos);
        }
        if (result instanceof Result.Error) {
            showMessage(viewModel.getErrorMessage(((Result.Error<Void>) result).code, isReturn));
            return;
        }
        finish();
    }

    private void showMessage(int message) {
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }
}
