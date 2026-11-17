package com.turbo.mobile.ui.admin;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.ActivityInspectionPhotosBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.admin.viewmodels.AllBookingsViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.util.List;

/**
 * Activity displaying pickup and return inspection photos side by side for Admin review.
 */
public class InspectionPhotosActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private ActivityInspectionPhotosBinding binding;
    private AllBookingsViewModel viewModel;
    private ImageStorage imageStorage;
    private long bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInspectionPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new AllBookingsViewModel(
                appContainer.bookingRepository,
                appContainer.paymentRepository,
                appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        binding.inspectionTitle.setText(getString(R.string.inspection_photos_title, bookingId));

        loadPhotos();
    }

    private void loadPhotos() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<InspectionPhotos> result = viewModel.getInspectionPhotos(bookingId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<InspectionPhotos>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        InspectionPhotos photos = ((Result.Success<InspectionPhotos>) result).data;
        fillPhotoColumn(binding.pickupList, photos.getPickupPaths());
        fillPhotoColumn(binding.returnList, photos.getReturnPaths());
    }

    private void fillPhotoColumn(LinearLayout column, List<String> paths) {
        column.removeAllViews();
        if (paths == null || paths.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.inspection_photos_none);
            emptyText.setTextColor(getColor(R.color.turbo_text_secondary));
            column.addView(emptyText);
            return;
        }

        int heightPx = Math.round(160 * getResources().getDisplayMetrics().density);
        int marginPx = Math.round(8 * getResources().getDisplayMetrics().density);

        for (String path : paths) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, heightPx);
            params.bottomMargin = marginPx;
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_turbo_card);
            imageView.setClipToOutline(true);

            Bitmap bitmap = imageStorage.load(path, 400);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_photo_placeholder);
            }
            column.addView(imageView);
        }
    }
}
