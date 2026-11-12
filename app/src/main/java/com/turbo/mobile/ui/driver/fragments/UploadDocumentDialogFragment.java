package com.turbo.mobile.ui.driver.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.DialogUploadDocumentBinding;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.MyDocumentsActivity;
import com.turbo.mobile.ui.driver.viewmodels.MyDocumentsViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.io.IOException;
import java.util.Calendar;

public class UploadDocumentDialogFragment extends DialogFragment {

    public static final String ARG_DOCUMENT_TYPE = "document_type";

    private static final int DAYS_IN_LONGEST_MONTH = 31;
    private static final int YEARS_AHEAD = 11;

    private DialogUploadDocumentBinding binding;
    private MyDocumentsViewModel viewModel;
    private ImageStorage imageStorage;
    private DocumentType documentType;
    private int firstYear;
    private Uri cameraUri;

    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), photoTaken -> {
                if (photoTaken) {
                    upload(cameraUri);
                }
            });

    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    upload(uri);
                }
            });

    public UploadDocumentDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogUploadDocumentBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new MyDocumentsViewModel(appContainer.documentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        documentType = DocumentType.valueOf(requireArguments().getString(ARG_DOCUMENT_TYPE));
        if (documentType == DocumentType.DRIVERS_LICENSE) {
            binding.uploadTitle.setText(R.string.my_documents_upload_license);
        } else {
            binding.uploadTitle.setText(R.string.my_documents_upload_study_permit);
        }

        setUpExpirySpinners();
        binding.takePhoto.setOnClickListener(view -> openCamera());
        binding.chooseFromGallery.setOnClickListener(view -> pickPhoto.launch("image/*"));

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        requireDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpExpirySpinners() {
        String[] days = new String[DAYS_IN_LONGEST_MONTH];
        for (int dayIndex = 0; dayIndex < DAYS_IN_LONGEST_MONTH; dayIndex++) {
            days[dayIndex] = String.valueOf(dayIndex + 1);
        }
        ScreenSetup.fillSpinner(binding.expiryDay, days);
        ScreenSetup.fillSpinner(binding.expiryMonth, getResources().getStringArray(R.array.my_documents_months));

        Calendar today = Calendar.getInstance();
        firstYear = today.get(Calendar.YEAR);
        String[] years = new String[YEARS_AHEAD];
        for (int yearIndex = 0; yearIndex < YEARS_AHEAD; yearIndex++) {
            years[yearIndex] = String.valueOf(firstYear + yearIndex);
        }
        ScreenSetup.fillSpinner(binding.expiryYear, years);

        binding.expiryDay.setSelection(today.get(Calendar.DAY_OF_MONTH) - 1);
        binding.expiryMonth.setSelection(today.get(Calendar.MONTH));
        binding.expiryYear.setSelection(1);
    }

    private void openCamera() {
        binding.errorBanner.setVisibility(View.GONE);
        try {
            cameraUri = imageStorage.createCameraUri();
            takePhoto.launch(cameraUri);
        } catch (IOException exception) {
            binding.errorBanner.setText(R.string.my_documents_error_file);
            binding.errorBanner.setVisibility(View.VISIBLE);
        }
    }

    private void upload(Uri image) {
        binding.errorBanner.setVisibility(View.GONE);
        int day = binding.expiryDay.getSelectedItemPosition() + 1;
        int month = binding.expiryMonth.getSelectedItemPosition();
        int year = firstYear + binding.expiryYear.getSelectedItemPosition();
        long expiryDate = viewModel.getExpiryDate(year, month, day);

        Result<Void> result = viewModel.upload(documentType, image, expiryDate);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        ((MyDocumentsActivity) requireActivity()).documentUploaded(documentType);
        dismiss();
    }
}
