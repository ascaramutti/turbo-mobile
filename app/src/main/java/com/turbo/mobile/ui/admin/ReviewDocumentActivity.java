package com.turbo.mobile.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
import com.turbo.mobile.databinding.ActivityReviewDocumentBinding;
import com.turbo.mobile.ui.TurboUi;

public class ReviewDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_KIND = "EXTRA_KIND";
    public static final String EXTRA_DOCUMENT_ID = "EXTRA_DOCUMENT_ID";

    private ActivityReviewDocumentBinding binding;
    private AppContainer container;
    private DocumentKind kind;
    private long documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);

        container = ((TurboApp) getApplication()).getContainer();
        kind = DocumentKind.valueOf(getIntent().getStringExtra(EXTRA_KIND));
        documentId = getIntent().getLongExtra(EXTRA_DOCUMENT_ID, 0);
        loadDocument();

        binding.btnApprove.setOnClickListener(v -> approve());

        binding.btnReject.setOnClickListener(v -> binding.overlayReject.setVisibility(View.VISIBLE));

        binding.btnCancel.setOnClickListener(v -> {
            binding.overlayReject.setVisibility(View.GONE);
            binding.etReason.setText("");
        });

        binding.etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasReason = s.toString().trim().length() > 0;
                binding.btnConfirmReject.setEnabled(hasReason);
                binding.txtReasonError.setVisibility(hasReason ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnConfirmReject.setOnClickListener(v -> {
            binding.overlayReject.setVisibility(View.GONE);
            reject(binding.etReason.getText().toString());
        });
    }

    private void loadDocument() {
        container.executor.execute(() -> {
            Result<DocumentReviewContext> result = container.documentRepository.getDocumentForReview(kind, documentId);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    showDocument(((Result.Success<DocumentReviewContext>) result).data);
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }

    private void showDocument(DocumentReviewContext document) {
        String name = TurboUi.documentName(this, document.getType());
        binding.txtPreview.setText(name);
        binding.boxDocument.setClipToOutline(true); // cuts the photo to the card's rounded corners
        TurboUi.loadPhoto(this, binding.imgDocument, document.getFilePath());
        StatusChip.apply(binding.chipStatus, TurboUi.documentStatusName(this, document.getStatus()));
        StatusChip.apply(binding.chipOther, TurboUi.documentStatusName(this, document.getOtherDocumentStatus()));

        if (kind == DocumentKind.DRIVER) {
            // "Study permit · Ana Lopez", its number and expiry, and the driver's other document.
            binding.txtSubtitle.setText(getString(R.string.twoPartsFormat, name, document.getDriverName()));
            boolean isLicense = document.getType() == DocumentType.DRIVERS_LICENSE;
            binding.txtFirstLabel.setText(isLicense ? R.string.lblLicenseNumber : R.string.lblPermitNumber);
            binding.txtFirstValue.setText(document.getDocumentNumber());
            binding.txtSecondLabel.setText(R.string.label_expiry);
            Long expiry = document.getExpiryDate();
            binding.txtSecondValue.setText(expiry == null ? "-" : TurboUi.fullDate(expiry));
            DocumentType other = isLicense ? DocumentType.STUDY_PERMIT : DocumentType.DRIVERS_LICENSE;
            binding.txtOtherName.setText(TurboUi.documentName(this, other));
            if (isLicense) {
                binding.txtOtherDetail.setVisibility(View.GONE); // "Class 4" belongs under the license card
            } else {
                int licenseClass = document.getLicenseClass() == LicenseClass.CLASS_4 ? 4 : 5;
                binding.txtOtherDetail.setText(getString(R.string.licenseClassFormat, licenseClass));
            }
        } else {
            // "Insurance certificate · Honda Fit 2021", its plate and VIN, and the vehicle's other document.
            binding.txtSubtitle.setText(getString(R.string.twoPartsFormat, name, document.getVehicleTitle()));
            binding.txtFirstLabel.setText(R.string.label_plate);
            binding.txtFirstValue.setText(document.getPlate());
            binding.txtSecondLabel.setText(R.string.label_vin);
            binding.txtSecondValue.setText(document.getVin());
            DocumentType other = document.getType() == DocumentType.INSURANCE
                    ? DocumentType.REGISTRATION
                    : DocumentType.INSURANCE;
            binding.txtOtherName.setText(TurboUi.documentName(this, other));
            binding.txtOtherDetail.setVisibility(View.GONE); // "Class 4" is only for drivers
        }
    }

    private void approve() {
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<ReviewOutcome> result = container.documentRepository.approve(adminId, kind, documentId);
            runOnUiThread(() -> closeOrShowError(result, R.string.documentApproved));
        });
    }

    private void reject(String reason) {
        long adminId = container.sessionManager.getUserId();
        container.executor.execute(() -> {
            Result<Void> result = container.documentRepository.reject(adminId, kind, documentId, reason);
            runOnUiThread(() -> closeOrShowError(result, R.string.documentRejected));
        });
    }

    // Done: back to "Pending documents", which reloads its list. Failed: the repository's message.
    private void closeOrShowError(Result<?> result, int doneMessage) {
        if (result instanceof Result.Success) {
            Toast.makeText(this, doneMessage, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            TurboUi.showError(this, result);
        }
    }
}
