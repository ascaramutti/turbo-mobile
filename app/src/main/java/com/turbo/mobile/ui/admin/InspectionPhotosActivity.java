package com.turbo.mobile.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.ActivityInspectionPhotosBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

public class InspectionPhotosActivity extends AppCompatActivity {

    private ActivityInspectionPhotosBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInspectionPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);

        long bookingId = getIntent().getLongExtra(BookingDetailActivity.EXTRA_BOOKING_ID, 0);
        binding.txtBookingNumber.setText(getString(R.string.bookingNumberFormat, bookingId));

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<InspectionPhotos> result = container.bookingRepository.getInspectionPhotos(adminId, bookingId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    InspectionPhotos photos = ((Result.Success<InspectionPhotos>) result).data;
                    showPhotos(binding.pickupGrid, photos.getPickupPaths());
                    showPhotos(binding.returnGrid, photos.getReturnPaths());
                    binding.txtSummary.setText(getString(R.string.inspectionSummaryFormat,
                            photos.getPickupPaths().size(), photos.getReturnPaths().size()));
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    // One tile per photo, without the × of the screens where you choose photos.
    private void showPhotos(ViewGroup grid, List<String> paths) {
        for (String path : paths) {
            View tile = getLayoutInflater().inflate(R.layout.item_photo_tile, grid, false);
            tile.setClipToOutline(true); // cuts the photo to the tile's rounded corners
            tile.findViewById(R.id.photo_remove).setVisibility(View.GONE);
            TurboUi.loadPhoto(this, tile.findViewById(R.id.photo_image), path);
            grid.addView(tile);
        }
    }
}
