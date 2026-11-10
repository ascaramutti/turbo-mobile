package com.turbo.mobile.ui.admin;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
import com.turbo.mobile.databinding.ActivityDocumentReviewBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.admin.fragments.ReasonDialogFragment;
import com.turbo.mobile.ui.admin.viewmodels.AdminViewModel;
import com.turbo.mobile.util.ImageStorage;

public class DocumentReviewActivity extends AppCompatActivity {

    public static final String EXTRA_KIND = "extra_kind";
    public static final String EXTRA_DOCUMENT_ID = "extra_document_id";

    private ActivityDocumentReviewBinding binding;
    private AdminViewModel viewModel;
    private ImageStorage imageStorage;
    private DocumentKind kind;
    private long documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new AdminViewModel(appContainer.documentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        String kindStr = getIntent().getStringExtra(EXTRA_KIND);
        kind = kindStr != null ? DocumentKind.valueOf(kindStr) : DocumentKind.DRIVER;
        documentId = getIntent().getLongExtra(EXTRA_DOCUMENT_ID, 0);

        binding.btnApprove.setOnClickListener(v -> approve());
        binding.btnReject.setOnClickListener(v -> showRejectDialog());

        loadDocument();
    }

    private void loadDocument() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<DocumentReviewContext> result = viewModel.getDocumentForReview(kind, documentId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<DocumentReviewContext>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            binding.btnApprove.setEnabled(false);
            binding.btnReject.setEnabled(false);
            return;
        }

        DocumentReviewContext doc = ((Result.Success<DocumentReviewContext>) result).data;
        populateView(doc);
    }

    private void populateView(DocumentReviewContext doc) {
        String typeName = getDocumentTypeName(doc.getType());
        binding.reviewTitle.setText(typeName);

        String path = doc.getFilePath();
        if (path != null && !path.isEmpty()) {
            Bitmap bitmap = imageStorage.load(path, 800);
            if (bitmap != null) {
                binding.documentImage.setImageBitmap(bitmap);
            } else {
                binding.documentImage.setImageResource(R.drawable.ic_file_text);
            }
        } else {
            binding.documentImage.setImageResource(R.drawable.ic_file_text);
        }

        if (kind == DocumentKind.DRIVER) {
            binding.cardTitle.setText(R.string.document_review_driver_header);
            binding.driverFields.setVisibility(View.VISIBLE);
            binding.vehicleFields.setVisibility(View.GONE);

            binding.driverName.setText(getString(R.string.document_review_driver_name, doc.getDriverName() != null ? doc.getDriverName() : "-"));
            binding.documentNumber.setText(getString(R.string.document_review_document_number, doc.getDocumentNumber() != null ? doc.getDocumentNumber() : "-"));

            if (doc.getType() == DocumentType.DRIVERS_LICENSE && doc.getLicenseClass() != null) {
                binding.licenseClass.setVisibility(View.VISIBLE);
                binding.licenseClass.setText(getString(R.string.document_review_license_class, doc.getLicenseClass().name()));
            } else {
                binding.licenseClass.setVisibility(View.GONE);
            }

            if (doc.getExpiryDate() != null) {
                binding.expiryDate.setText(getString(R.string.document_review_expiry_date, DisplayFormat.fullDate(doc.getExpiryDate())));
            } else {
                binding.expiryDate.setText(getString(R.string.document_review_expiry_date, "-"));
            }
        } else {
            binding.cardTitle.setText(R.string.document_review_vehicle_header);
            binding.driverFields.setVisibility(View.GONE);
            binding.vehicleFields.setVisibility(View.VISIBLE);

            binding.vehicleTitle.setText(getString(R.string.document_review_vehicle_title, doc.getVehicleTitle() != null ? doc.getVehicleTitle() : "-"));
            binding.ownerName.setText(getString(R.string.document_review_owner_name, doc.getOwnerName() != null ? doc.getOwnerName() : "-"));
            binding.vehicleVin.setText(getString(R.string.document_review_vin, doc.getVin() != null ? doc.getVin() : "-"));
            binding.vehiclePlate.setText(getString(R.string.document_review_plate, doc.getPlate() != null ? doc.getPlate() : "-"));
        }

        String otherName = getOtherDocumentTypeName(doc.getType());
        String otherStatus = getStatusText(doc.getOtherDocumentStatus());
        binding.otherDocumentStatus.setText(getString(R.string.document_review_other_document, otherName + " (" + otherStatus + ")"));
    }

    private void approve() {
        binding.errorBanner.setVisibility(View.GONE);
        Result<ReviewOutcome> result = viewModel.approve(kind, documentId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<ReviewOutcome>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        Toast.makeText(this, R.string.document_review_approved_toast, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showRejectDialog() {
        ReasonDialogFragment dialog = ReasonDialogFragment.newInstance(kind, documentId);
        dialog.show(getSupportFragmentManager(), "reason_dialog");
    }

    public void onDocumentRejected() {
        Toast.makeText(this, R.string.document_review_rejected_toast, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getDocumentTypeName(DocumentType type) {
        if (type == null) return "";
        switch (type) {
            case DRIVERS_LICENSE:
                return getString(R.string.document_type_drivers_license);
            case STUDY_PERMIT:
                return getString(R.string.document_type_study_permit);
            case INSURANCE:
                return getString(R.string.document_type_insurance);
            case REGISTRATION:
                return getString(R.string.document_type_registration);
            default:
                return type.name();
        }
    }

    private String getOtherDocumentTypeName(DocumentType type) {
        if (type == null) return "";
        switch (type) {
            case DRIVERS_LICENSE:
                return getString(R.string.document_type_study_permit);
            case STUDY_PERMIT:
                return getString(R.string.document_type_drivers_license);
            case INSURANCE:
                return getString(R.string.document_type_registration);
            case REGISTRATION:
                return getString(R.string.document_type_insurance);
            default:
                return "";
        }
    }

    private String getStatusText(DocumentStatus status) {
        if (status == null) {
            return getString(R.string.document_status_not_uploaded);
        }
        switch (status) {
            case APPROVED:
                return getString(R.string.vehicle_form_document_approved);
            case REJECTED:
                return getString(R.string.vehicle_form_document_rejected);
            case PENDING:
                return getString(R.string.vehicle_form_document_pending);
            default:
                return status.name();
        }
    }
}
