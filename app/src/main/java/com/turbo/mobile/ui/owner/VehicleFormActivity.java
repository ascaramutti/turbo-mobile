package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentAction;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.FuelType;
import com.turbo.mobile.data.model.PhotoItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleDocumentItem;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.databinding.ActivityVehicleFormBinding;
import com.turbo.mobile.databinding.ItemPhotoTileBinding;
import com.turbo.mobile.databinding.ItemVehicleDocumentBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.owner.viewmodels.VehicleFormViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.util.ArrayList;
import java.util.List;

public class VehicleFormActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private static final int PHOTO_SIZE = 300;

    private ActivityVehicleFormBinding binding;
    private VehicleFormViewModel viewModel;
    private ImageStorage imageStorage;
    private long vehicleId;
    private VehicleForm form = new VehicleForm();
    private List<Zone> zones;
    private final List<PhotoItem> photos = new ArrayList<>();
    private Uri insurance;
    private Uri registration;
    private DocumentType uploadType;

    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    addPhoto(uri);
                }
            });

    private final ActivityResultLauncher<String> pickInsurance =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    insurance = uri;
                    binding.pickInsurance.setText(R.string.vehicle_form_insurance_added);
                }
            });

    private final ActivityResultLauncher<String> pickRegistration =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    registration = uri;
                    binding.pickRegistration.setText(R.string.vehicle_form_registration_added);
                }
            });

    private final ActivityResultLauncher<String> pickDocument =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadDocument(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVehicleFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new VehicleFormViewModel(appContainer.vehicleRepository, appContainer.documentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        setUpSpinners();
        binding.addPhoto.setOnClickListener(v -> pickPhoto.launch("image/*"));
        binding.pickInsurance.setOnClickListener(v -> pickInsurance.launch("image/*"));
        binding.pickRegistration.setOnClickListener(v -> pickRegistration.launch("image/*"));
        binding.restore.setOnClickListener(v -> restore());
        binding.save.setOnClickListener(v -> save());

        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0);
        if (vehicleId != 0) {
            binding.inputVin.setEnabled(false);
            binding.documents.setVisibility(View.GONE);
            binding.editDocuments.setVisibility(View.VISIBLE);
            binding.save.setText(R.string.vehicle_form_save_changes);
            loadVehicle();
            showDocuments();
        }
        showPhotos();
    }

    private void setUpSpinners() {
        ScreenSetup.fillSpinner(binding.spinnerCategory, getResources().getStringArray(R.array.vehicle_category_names));
        ScreenSetup.fillSpinner(binding.spinnerFuel, getResources().getStringArray(R.array.vehicle_fuel_names));
        ScreenSetup.fillSpinner(binding.spinnerService, getResources().getStringArray(R.array.vehicle_service_names));

        zones = viewModel.getZones();
        String[] zoneNames = new String[zones.size()];
        for (int i = 0; i < zones.size(); i++) {
            zoneNames[i] = zones.get(i).getName();
        }
        ScreenSetup.fillSpinner(binding.spinnerZone, zoneNames);

        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean compact = VehicleCategory.values()[position] == VehicleCategory.COMPACT;
                if (compact) {
                    binding.spinnerService.setSelection(VehicleServiceType.DELIVERY_ONLY.ordinal());
                }
                binding.spinnerService.setEnabled(!compact);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadVehicle() {
        Result<VehicleEditData> result = viewModel.loadForEdit(vehicleId);
        if (result instanceof Result.Error) {
            showError(((Result.Error<VehicleEditData>) result).code);
            binding.save.setVisibility(View.GONE);
            return;
        }
        VehicleEditData data = ((Result.Success<VehicleEditData>) result).data;
        form = data.getForm();
        photos.addAll(data.getPhotos());

        binding.formTitle.setText(getString(R.string.vehicle_form_edit_title, form.getMake(), form.getModel()));
        binding.inputMake.setText(form.getMake());
        binding.inputModel.setText(form.getModel());
        binding.inputYear.setText(String.valueOf(form.getYear()));
        binding.inputPlate.setText(form.getPlate());
        binding.inputVin.setText(form.getVin());
        binding.spinnerCategory.setSelection(form.getCategory().ordinal());
        binding.spinnerFuel.setSelection(form.getFuelType().ordinal());
        binding.spinnerService.setSelection(form.getServiceType().ordinal());
        binding.inputRate.setText(viewModel.toRateText(form.getHourlyRateCents()));
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getId() == form.getZoneId()) {
                binding.spinnerZone.setSelection(i);
            }
        }
        binding.inputAddress.setText(form.getAddress());
    }

    private void showPhotos() {
        binding.photoList.removeAllViews();
        for (PhotoItem photo : photos) {
            ItemPhotoTileBinding tile = ItemPhotoTileBinding.inflate(getLayoutInflater(), binding.photoList, false);
            tile.getRoot().setClipToOutline(true);
            tile.photoImage.setImageBitmap(imageStorage.load(photo.getFilePath(), PHOTO_SIZE));
            tile.photoRemove.setOnClickListener(v -> removePhoto(photo));
            binding.photoList.addView(tile.getRoot());
        }
        binding.photoMinimum.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addPhoto(Uri uri) {
        hideBanners();
        PhotoItem photo = new PhotoItem();
        photo.setFilePath(uri.toString());
        if (vehicleId != 0) {
            Result<Long> result = viewModel.addPhoto(vehicleId, uri);
            if (result instanceof Result.Error) {
                showError(((Result.Error<Long>) result).code);
                return;
            }
            photo.setId(((Result.Success<Long>) result).data);
        }
        photos.add(photo);
        showPhotos();
    }

    private void removePhoto(PhotoItem photo) {
        hideBanners();
        if (vehicleId != 0) {
            Result<Void> result = viewModel.deletePhoto(photo.getId());
            if (result instanceof Result.Error) {
                showError(((Result.Error<Void>) result).code);
                return;
            }
        }
        photos.remove(photo);
        showPhotos();
    }

    private void readForm() {
        form.setMake(binding.inputMake.getText().toString().trim());
        form.setModel(binding.inputModel.getText().toString().trim());
        form.setYear(viewModel.toNumber(binding.inputYear.getText().toString()));
        form.setPlate(binding.inputPlate.getText().toString().trim());
        form.setVin(binding.inputVin.getText().toString().trim());
        form.setCategory(VehicleCategory.values()[binding.spinnerCategory.getSelectedItemPosition()]);
        form.setFuelType(FuelType.values()[binding.spinnerFuel.getSelectedItemPosition()]);
        form.setServiceType(VehicleServiceType.values()[binding.spinnerService.getSelectedItemPosition()]);
        form.setHourlyRateCents(viewModel.toCents(binding.inputRate.getText().toString()));
        form.setZoneId(zones.get(binding.spinnerZone.getSelectedItemPosition()).getId());
        form.setAddress(binding.inputAddress.getText().toString().trim());
    }

    private void save() {
        hideBanners();
        readForm();
        if (vehicleId == 0) {
            register();
        } else {
            update();
        }
    }

    private void register() {
        List<Uri> photoUris = new ArrayList<>();
        for (PhotoItem photo : photos) {
            photoUris.add(Uri.parse(photo.getFilePath()));
        }
        Result<Long> result = viewModel.register(form, photoUris, insurance, registration, binding.notSalvage.isChecked());
        if (result instanceof Result.Success) {
            finish();
        } else {
            showError(((Result.Error<Long>) result).code);
        }
    }

    private void update() {
        Result<Void> result = viewModel.update(vehicleId, form, photos.size());
        if (result instanceof Result.Success) {
            finish();
        } else {
            showError(((Result.Error<Void>) result).code);
        }
    }

    private void restore() {
        hideBanners();
        Result<Long> result = viewModel.restore(form.getVin());
        if (result instanceof Result.Error) {
            showError(((Result.Error<Long>) result).code);
            return;
        }
        Intent intent = new Intent(this, VehicleFormActivity.class);
        intent.putExtra(EXTRA_VEHICLE_ID, ((Result.Success<Long>) result).data);
        startActivity(intent);
        finish();
    }

    private void showDocuments() {
        binding.documentList.removeAllViews();
        for (VehicleDocumentItem document : viewModel.getDocuments(vehicleId)) {
            ItemVehicleDocumentBinding card = ItemVehicleDocumentBinding.inflate(getLayoutInflater(), binding.documentList, false);
            card.documentTitle.setText(documentName(document.getType()));
            switch (document.getStatus()) {
                case REJECTED:
                    card.documentDetail.setText(getString(R.string.vehicle_form_document_reason, document.getRejectionReason()));
                    paintChip(card.documentStatus, R.string.vehicle_form_document_rejected, R.color.turbo_error_bg, R.color.turbo_error);
                    break;
                case PENDING:
                    card.documentDetail.setText(getString(R.string.vehicle_form_document_uploaded, DisplayFormat.date(document.getUploadedAt())));
                    paintChip(card.documentStatus, R.string.vehicle_form_document_pending, R.color.turbo_warning_bg, R.color.turbo_warning);
                    break;
                case APPROVED:
                    card.documentDetail.setText(R.string.vehicle_form_document_current);
                    paintChip(card.documentStatus, R.string.vehicle_form_document_approved, R.color.turbo_success_bg, R.color.turbo_success);
                    break;
            }
            if (document.getAllowedAction() == DocumentAction.REUPLOAD) {
                card.documentAction.setText(R.string.vehicle_form_reupload);
                card.documentAction.setOnClickListener(v -> pickDocumentFor(document.getType()));
            } else if (document.getAllowedAction() == DocumentAction.REPLACE) {
                card.documentAction.setText(R.string.vehicle_form_replace);
                card.documentAction.setOnClickListener(v -> pickDocumentFor(document.getType()));
            } else {
                card.documentAction.setVisibility(View.GONE);
            }
            binding.documentList.addView(card.getRoot());
        }
    }

    private int documentName(DocumentType type) {
        if (type == DocumentType.INSURANCE) {
            return R.string.vehicle_form_insurance_title;
        }
        return R.string.vehicle_form_registration_title;
    }

    private void paintChip(TextView chip, int text, int backgroundColor, int textColor) {
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(this, backgroundColor));
        chip.setTextColor(ContextCompat.getColor(this, textColor));
    }

    private void pickDocumentFor(DocumentType type) {
        uploadType = type;
        pickDocument.launch("image/*");
    }

    private void uploadDocument(Uri image) {
        hideBanners();
        Result<Void> result = viewModel.uploadDocument(vehicleId, uploadType, image);
        if (result instanceof Result.Success) {
            binding.successBanner.setText(getString(R.string.vehicle_form_document_sent, getString(documentName(uploadType))));
            binding.successBanner.setVisibility(View.VISIBLE);
            binding.formScroll.scrollTo(0, 0);
        } else {
            showError(((Result.Error<Void>) result).code);
        }
        showDocuments();
    }

    private void showError(RepoError code) {
        binding.formScroll.scrollTo(0, 0);
        if (code == VehicleError.VIN_RESTORABLE) {
            binding.restoreBanner.setVisibility(View.VISIBLE);
            binding.restore.setVisibility(View.VISIBLE);
            return;
        }
        int message = R.string.error_unexpected;
        if (code == VehicleError.INVALID_INPUT) {
            message = R.string.vehicle_form_error_required;
        } else if (code == VehicleError.INVALID_VIN) {
            message = R.string.vehicle_form_error_invalid_vin;
        } else if (code == VehicleError.VIN_TAKEN) {
            message = R.string.vehicle_form_error_vin_taken;
        } else if (code == VehicleError.PLATE_TAKEN) {
            message = R.string.vehicle_form_error_plate_taken;
        } else if (code == VehicleError.TOO_OLD) {
            message = R.string.vehicle_form_error_too_old;
        } else if (code == VehicleError.COMPACT_DELIVERY_ONLY) {
            message = R.string.vehicle_form_error_compact;
        } else if (code == VehicleError.PHOTO_REQUIRED) {
            message = R.string.vehicle_form_error_photo_required;
        } else if (code == VehicleError.DOCUMENTS_REQUIRED) {
            message = R.string.vehicle_form_error_documents_required;
        } else if (code == VehicleError.LAST_PHOTO) {
            message = R.string.vehicle_form_error_last_photo;
        } else if (code == DocumentError.ACTION_NOT_ALLOWED) {
            message = R.string.vehicle_form_error_approved_document;
        } else if (code == VehicleError.FILE_ERROR || code == DocumentError.FILE_ERROR) {
            message = R.string.vehicle_form_error_file;
        } else if (code == VehicleError.VEHICLE_DELETED || code == DocumentError.VEHICLE_DELETED) {
            message = R.string.vehicle_form_error_deleted;
        } else if (code == VehicleError.NOT_FOUND || code == DocumentError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == VehicleError.NOT_ALLOWED || code == DocumentError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        }
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }

    private void hideBanners() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);
        binding.restoreBanner.setVisibility(View.GONE);
        binding.restore.setVisibility(View.GONE);
    }
}
