package com.turbo.mobile.ui.driver;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.TurboUi;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/** W-13 My documents: the driver's license and study permit, their review status and the re-upload of a rejected one. */
public class MyDocumentsActivity extends AppCompatActivity {

    private AppContainer container;
    private long driverId;
    private DocumentType rejectedType; // the document to send again, or null if none was rejected
    private Uri newPhoto; // the new photo of the rejected document, null until chosen
    private Long expiryDate; // its expiry date in milliseconds, null until chosen
    private Uri cameraUri; // where the camera saves the photo being taken

    private TextView subtitle;
    private View photoOptions;
    private TextView takePhotoChip;
    private TextView galleryChip;
    private TextView successBanner;
    private TextView warningBanner;
    private TextView errorBanner;
    private View reuploadForm;
    private TextView selectedPhoto;
    private TextView expiryButton;
    private LinearLayout documentList;
    private View submitBar;
    private Button submitButton;

    // "Take photo": opens the camera app, which saves the photo in cameraUri.
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), taken -> {
                if (taken) {
                    photoChosen(cameraUri, takePhotoChip);
                }
            });

    // "Choose from gallery": opens the phone's gallery ("image/*" = any image).
    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoChosen(uri, galleryChip);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_my_documents);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        driverId = container.sessionManager.getUserId();

        subtitle = findViewById(R.id.documents_subtitle);
        photoOptions = findViewById(R.id.photo_options);
        takePhotoChip = findViewById(R.id.take_photo);
        galleryChip = findViewById(R.id.choose_gallery);
        successBanner = findViewById(R.id.banner_success);
        warningBanner = findViewById(R.id.banner_warning);
        errorBanner = findViewById(R.id.banner_error);
        reuploadForm = findViewById(R.id.reupload_form);
        selectedPhoto = findViewById(R.id.selected_photo);
        expiryButton = findViewById(R.id.pick_expiry);
        documentList = findViewById(R.id.document_list);
        submitBar = findViewById(R.id.submit_bar);
        submitButton = findViewById(R.id.submit_document);

        takePhotoChip.setOnClickListener(v -> openCamera());
        galleryChip.setOnClickListener(v -> pickPhoto.launch("image/*"));
        expiryButton.setOnClickListener(v -> pickExpiry());
        submitButton.setOnClickListener(v -> submit());

        loadDocuments();
    }

    private void loadDocuments() {
        container.executor.execute(() -> {
            List<DriverDocumentItem> documents = container.documentRepository.getDriverDocuments(driverId);
            runOnUiThread(() -> showDocuments(documents));
        });
    }

    private void showDocuments(List<DriverDocumentItem> documents) {
        documentList.removeAllViews();
        rejectedType = null;
        boolean allApproved = true;
        for (DriverDocumentItem document : documents) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, documentList, false);
            fillCard(this, card, document);
            documentList.addView(card);
            if (document.getStatus() == DocumentStatus.REJECTED) {
                rejectedType = document.getType();
            }
            if (document.getStatus() != DocumentStatus.APPROVED) {
                allApproved = false;
            }
        }

        // Three versions: rejected (send it again), all approved (can book) or waiting for the review.
        successBanner.setVisibility(View.GONE);
        warningBanner.setVisibility(View.GONE);
        errorBanner.setVisibility(View.GONE);
        if (rejectedType != null) {
            subtitle.setVisibility(View.GONE);
            errorBanner.setVisibility(View.VISIBLE);
        } else if (allApproved) {
            subtitle.setText(R.string.documents_ready);
            subtitle.setVisibility(View.VISIBLE);
            successBanner.setVisibility(View.VISIBLE);
        } else {
            subtitle.setText(R.string.documents_in_review);
            subtitle.setVisibility(View.VISIBLE);
            warningBanner.setVisibility(View.VISIBLE);
        }
        int reupload = View.GONE;
        if (rejectedType != null) {
            reupload = View.VISIBLE;
        }
        photoOptions.setVisibility(reupload);
        reuploadForm.setVisibility(reupload);
        submitBar.setVisibility(reupload);
    }

    // Fills one document card. Also used by W-06 when the driver can't book yet.
    static void fillCard(Activity activity, View card, DriverDocumentItem document) {
        TextView title = card.findViewById(R.id.card_title);
        TextView subtitle = card.findViewById(R.id.card_subtitle);
        TextView chip = card.findViewById(R.id.chip_primary);
        TextView footer = card.findViewById(R.id.card_footer);

        if (document.getType() == DocumentType.DRIVERS_LICENSE) {
            title.setText(R.string.drivers_license);
        } else {
            title.setText(R.string.study_permit);
        }

        if (document.getStatus() == null) { // never uploaded
            subtitle.setText(R.string.not_uploaded);
            footer.setVisibility(View.GONE);
            return;
        }
        switch (document.getStatus()) {
            case APPROVED:
                // "Expires May 20, 2028"
                subtitle.setText(activity.getString(R.string.expires_on, TurboUi.fullDate(document.getExpiryDate())));
                TurboUi.paintChip(chip, activity.getString(R.string.chip_approved),
                        R.color.turbo_success_bg, R.color.turbo_success);
                footer.setVisibility(View.GONE);
                break;
            case PENDING:
                // "Uploaded Oct 3"
                subtitle.setText(activity.getString(R.string.document_uploaded, TurboUi.date(document.getUploadedAt())));
                TurboUi.paintChip(chip, activity.getString(R.string.chip_pending_review),
                        R.color.turbo_warning_bg, R.color.turbo_warning);
                footer.setText(R.string.document_waiting);
                break;
            case REJECTED:
                // "Reason: Image is blurry"
                subtitle.setText(activity.getString(R.string.document_reason, document.getRejectionReason()));
                TurboUi.paintChip(chip, activity.getString(R.string.chip_rejected),
                        R.color.turbo_error_bg, R.color.turbo_error);
                footer.setText(R.string.reupload_required);
                break;
        }
        TurboUi.showCardPhoto(activity, card, document.getFilePath());
    }

    private void openCamera() {
        try {
            cameraUri = container.imageStorage.createCameraUri(); // an empty file for the camera to fill
            takePhoto.launch(cameraUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    // Remembers the new photo and marks the chip that was used (camera or gallery) in orange.
    private void photoChosen(Uri uri, TextView usedChip) {
        newPhoto = uri;
        selectedPhoto.setText(R.string.photo_selected);
        takePhotoChip.setSelected(usedChip == takePhotoChip);
        galleryChip.setSelected(usedChip == galleryChip);
    }

    // Android's calendar. Only future days: an expired document can't be approved.
    private void pickExpiry() {
        DatePickerDialog picker = new DatePickerDialog(this); // opens on today
        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.setOnDateSetListener((view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.set(year, month, day, 0, 0, 0);
            expiryDate = date.getTimeInMillis();
            expiryButton.setText(TurboUi.fullDate(expiryDate));
        });
        picker.show();
    }

    // Sends the new photo. The document goes back to "Pending review" and the screen reloads.
    private void submit() {
        if (newPhoto == null || expiryDate == null) {
            Toast.makeText(this, R.string.choose_photo_and_expiry, Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentType type = rejectedType;
        Uri photo = newPhoto;
        long expiry = expiryDate;
        submitButton.setEnabled(false); // no double taps while sending
        container.executor.execute(() -> {
            Result<Void> result = container.documentRepository.uploadDriverDocument(driverId, type, photo, expiry);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.document_sent, Toast.LENGTH_SHORT).show();
                    newPhoto = null;
                    expiryDate = null;
                    selectedPhoto.setText(R.string.no_photo_selected);
                    expiryButton.setText(R.string.choose_date);
                } else {
                    TurboUi.showError(this, result);
                }
                submitButton.setEnabled(true);
                loadDocuments();
            });
        });
    }
}
