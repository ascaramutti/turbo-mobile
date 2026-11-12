package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentAction;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.ActivityMyDocumentsBinding;
import com.turbo.mobile.databinding.ItemVehicleDocumentBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.fragments.UploadDocumentDialogFragment;
import com.turbo.mobile.ui.driver.viewmodels.MyDocumentsViewModel;

public class MyDocumentsActivity extends AppCompatActivity {

    private ActivityMyDocumentsBinding binding;
    private MyDocumentsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyDocumentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(view -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new MyDocumentsViewModel(appContainer.documentRepository, appContainer.sessionManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    private void loadDocuments() {
        showDriverStatus();
        showDocuments();
    }

    private void showDriverStatus() {
        binding.rejectedBanner.setVisibility(View.GONE);
        binding.pendingBanner.setVisibility(View.GONE);
        binding.errorBanner.setVisibility(View.GONE);

        Result<DriverStatus> result = viewModel.loadStatus();
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<DriverStatus>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        DriverState state = ((Result.Success<DriverStatus>) result).data.getState();
        if (state == DriverState.DOCUMENT_REJECTED) {
            binding.rejectedBanner.setVisibility(View.VISIBLE);
        } else if (state == DriverState.PENDING_VERIFICATION) {
            binding.pendingBanner.setVisibility(View.VISIBLE);
        }
    }

    private void showDocuments() {
        binding.documentList.removeAllViews();
        for (DriverDocumentItem document : viewModel.getDocuments()) {
            ItemVehicleDocumentBinding card = ItemVehicleDocumentBinding.inflate(getLayoutInflater(), binding.documentList, false);
            card.documentTitle.setText(documentName(document.getType()));

            if (document.getStatus() == null) {
                card.documentDetail.setText(R.string.my_documents_not_uploaded);
                card.documentStatus.setVisibility(View.GONE);
            } else if (document.getStatus() == DocumentStatus.REJECTED) {
                card.documentDetail.setText(getString(R.string.my_documents_reason, document.getRejectionReason()));
                paintChip(card.documentStatus, R.string.my_documents_rejected, R.color.turbo_error_bg, R.color.turbo_error);
            } else if (document.getStatus() == DocumentStatus.PENDING) {
                card.documentDetail.setText(getString(R.string.my_documents_uploaded, DisplayFormat.date(document.getUploadedAt())));
                paintChip(card.documentStatus, R.string.my_documents_pending, R.color.turbo_warning_bg, R.color.turbo_warning);
            } else {
                card.documentDetail.setText(expiryText(document));
                paintChip(card.documentStatus, R.string.my_documents_approved, R.color.turbo_success_bg, R.color.turbo_success);
            }

            if (document.getAllowedAction() == DocumentAction.NONE) {
                card.documentAction.setVisibility(View.GONE);
            } else {
                card.documentAction.setText(actionName(document.getAllowedAction()));
                card.documentAction.setOnClickListener(view -> openUpload(document.getType()));
            }
            binding.documentList.addView(card.getRoot());
        }
    }

    private String expiryText(DriverDocumentItem document) {
        if (document.getAllowedAction() != DocumentAction.RENEW) {
            return getString(R.string.my_documents_expires_on, DisplayFormat.fullDate(document.getExpiryDate()));
        }
        long daysLeft = viewModel.getDaysLeft(document.getExpiryDate());
        if (daysLeft > 0) {
            return getString(R.string.my_documents_expires_in, daysLeft);
        }
        return getString(R.string.my_documents_expired);
    }

    private int documentName(DocumentType type) {
        if (type == DocumentType.DRIVERS_LICENSE) {
            return R.string.my_documents_license;
        }
        return R.string.my_documents_study_permit;
    }

    private int actionName(DocumentAction action) {
        if (action == DocumentAction.REUPLOAD) {
            return R.string.my_documents_reupload;
        } else if (action == DocumentAction.REPLACE) {
            return R.string.my_documents_replace;
        } else if (action == DocumentAction.RENEW) {
            return R.string.my_documents_renew;
        }
        return R.string.my_documents_upload;
    }

    private void paintChip(TextView chip, int text, int backgroundColor, int textColor) {
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(this, backgroundColor));
        chip.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void openUpload(DocumentType type) {
        binding.successBanner.setVisibility(View.GONE);

        Bundle arguments = new Bundle();
        arguments.putString(UploadDocumentDialogFragment.ARG_DOCUMENT_TYPE, type.name());

        UploadDocumentDialogFragment dialog = new UploadDocumentDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getSupportFragmentManager(), "upload_document");
    }

    public void documentUploaded(DocumentType type) {
        loadDocuments();
        binding.successBanner.setText(getString(R.string.my_documents_sent, getString(documentName(type))));
        binding.successBanner.setVisibility(View.VISIBLE);
    }
}
