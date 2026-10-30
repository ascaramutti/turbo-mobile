package com.turbo.mobile.ui.owner;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleDocumentItem;

import java.util.List;

/** W-16 Vehicle documents: the insurance and the registration of one vehicle, and their review status. */
public class VehicleDocumentsActivity extends AppCompatActivity {

    // Names of the Intent extras sent by MyVehiclesFragment.
    public static final String EXTRA_VEHICLE_ID = "vehicle_id";
    public static final String EXTRA_VEHICLE_SUMMARY = "vehicle_summary";

    private AppContainer container;
    private long ownerId;
    private long vehicleId;
    private DocumentType rejectedType; // the document to re-upload, or null if none was rejected

    private LinearLayout documentList;
    private TextView rejectedBanner;
    private TextView pendingBanner;
    private TextView approvedBanner;
    private View reuploadBar;
    private Button reuploadButton;

    // Opens the gallery to choose the new photo of the rejected document.
    private final ActivityResultLauncher<String> pickDocument =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    upload(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OwnerUi.setUpScreen(this, R.layout.activity_vehicle_documents);
        OwnerUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        ownerId = container.sessionManager.getUserId();
        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);

        TextView summary = findViewById(R.id.vehicle_summary);
        summary.setText(getIntent().getStringExtra(EXTRA_VEHICLE_SUMMARY));
        documentList = findViewById(R.id.document_list);
        rejectedBanner = findViewById(R.id.banner_rejected);
        pendingBanner = findViewById(R.id.banner_pending);
        approvedBanner = findViewById(R.id.banner_approved);
        reuploadBar = findViewById(R.id.reupload_bar);
        reuploadButton = findViewById(R.id.reupload);
        reuploadButton.setOnClickListener(v -> pickDocument.launch("image/*"));

        loadDocuments();
    }

    private void loadDocuments() {
        container.executor.execute(() -> {
            List<VehicleDocumentItem> documents = container.documentRepository.getVehicleDocuments(vehicleId);
            runOnUiThread(() -> showDocuments(documents));
        });
    }

    private void showDocuments(List<VehicleDocumentItem> documents) {
        documentList.removeAllViews();
        rejectedType = null;
        int pendingCount = 0;
        String pendingName = "";

        for (VehicleDocumentItem document : documents) {
            View card = getLayoutInflater().inflate(R.layout.item_info_card, documentList, false);
            TextView title = card.findViewById(R.id.card_title);
            TextView subtitle = card.findViewById(R.id.card_subtitle);
            TextView chip = card.findViewById(R.id.chip_primary);
            TextView footer = card.findViewById(R.id.card_footer);
            String uploaded = getString(R.string.document_uploaded, OwnerUi.date(document.getUploadedAt()));

            if (document.getType() == DocumentType.INSURANCE) {
                title.setText(R.string.label_insurance);
            } else {
                title.setText(R.string.label_registration);
            }

            switch (document.getStatus()) {
                case REJECTED:
                    subtitle.setText(getString(R.string.document_reason, document.getRejectionReason()));
                    OwnerUi.paintChip(chip, getString(R.string.chip_rejected),
                            R.color.turbo_error_bg, R.color.turbo_error);
                    footer.setText(R.string.reupload);
                    if (rejectedType == null) {
                        rejectedType = document.getType();
                    }
                    break;
                case PENDING:
                    subtitle.setText(uploaded);
                    OwnerUi.paintChip(chip, getString(R.string.chip_pending_review),
                            R.color.turbo_warning_bg, R.color.turbo_warning);
                    footer.setText(R.string.document_waiting);
                    pendingCount = pendingCount + 1;
                    pendingName = documentShortName(document.getType());
                    break;
                case APPROVED:
                    subtitle.setText(R.string.document_current);
                    OwnerUi.paintChip(chip, getString(R.string.chip_approved),
                            R.color.turbo_success_bg, R.color.turbo_success);
                    footer.setText(uploaded);
                    break;
            }
            documentList.addView(card);
        }

        // One banner: rejected wins over pending, pending wins over approved.
        rejectedBanner.setVisibility(View.GONE);
        pendingBanner.setVisibility(View.GONE);
        approvedBanner.setVisibility(View.GONE);
        if (rejectedType != null) {
            rejectedBanner.setVisibility(View.VISIBLE);
        } else if (pendingCount == 1) {
            pendingBanner.setText(getString(R.string.documents_pending_banner, pendingName));
            pendingBanner.setVisibility(View.VISIBLE);
        } else if (pendingCount > 1) {
            pendingBanner.setText(R.string.documents_both_pending_banner);
            pendingBanner.setVisibility(View.VISIBLE);
        } else {
            approvedBanner.setVisibility(View.VISIBLE);
        }

        // "Re-upload insurance" or "Re-upload registration", only when a document was rejected.
        if (rejectedType == null) {
            reuploadBar.setVisibility(View.GONE);
        } else {
            if (rejectedType == DocumentType.INSURANCE) {
                reuploadButton.setText(R.string.reupload_insurance);
            } else {
                reuploadButton.setText(R.string.reupload_registration);
            }
            reuploadBar.setVisibility(View.VISIBLE);
        }
    }

    // "Insurance" or "Registration", for the pending banner.
    private String documentShortName(DocumentType type) {
        if (type == DocumentType.INSURANCE) {
            return getString(R.string.document_insurance);
        }
        return getString(R.string.document_registration);
    }

    // Sends the new photo. The document goes back to "Pending review" and the screen reloads.
    private void upload(Uri uri) {
        DocumentType type = rejectedType;
        reuploadButton.setEnabled(false); // no double taps while uploading
        container.executor.execute(() -> {
            Result<Void> result = container.documentRepository.uploadVehicleDocument(ownerId, vehicleId, type, uri);
            runOnUiThread(() -> {
                if (result instanceof Result.Error) {
                    OwnerUi.showError(this, result);
                }
                reuploadButton.setEnabled(true);
                loadDocuments();
            });
        });
    }
}
