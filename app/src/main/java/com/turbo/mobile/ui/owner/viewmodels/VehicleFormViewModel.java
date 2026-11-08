package com.turbo.mobile.ui.owner.viewmodels;

import android.net.Uri;

import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleDocumentItem;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.data.model.Zone;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VehicleFormViewModel {

    private static final int VIN_LENGTH = 17;
    private static final int MAX_AGE_YEARS = 9;

    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final SessionManager sessionManager;

    public VehicleFormViewModel(VehicleRepository vehicleRepository, DocumentRepository documentRepository, SessionManager sessionManager) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.sessionManager = sessionManager;
    }

    public List<Zone> getZones() {
        return vehicleRepository.getZones();
    }

    public Result<VehicleEditData> loadForEdit(long vehicleId) {
        return vehicleRepository.getVehicleForEdit(sessionManager.getUserId(), vehicleId);
    }

    public int toNumber(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int toCents(String text) {
        try {
            return (int) Math.round(Double.parseDouble(text.trim()) * 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String toRateText(int cents) {
        return String.format(Locale.US, "%.2f", cents / 100.0);
    }

    public VehicleError validateForm(VehicleForm form, int photoCount, boolean notSalvage) {
        if (form.getMake().isEmpty() || form.getModel().isEmpty() || form.getPlate().isEmpty()
                || form.getAddress().isEmpty() || form.getYear() <= 0 || form.getHourlyRateCents() <= 0
                || form.getZoneId() == 0 || !notSalvage) {
            return VehicleError.INVALID_INPUT;
        }
        String vin = form.getVin().toUpperCase();
        if (vin.length() != VIN_LENGTH || vin.contains("I") || vin.contains("O") || vin.contains("Q")) {
            return VehicleError.INVALID_VIN;
        }
        if (Calendar.getInstance().get(Calendar.YEAR) - form.getYear() >= MAX_AGE_YEARS) {
            return VehicleError.TOO_OLD;
        }
        if (form.getCategory() == VehicleCategory.COMPACT && form.getServiceType() == VehicleServiceType.TAXI_AND_DELIVERY) {
            return VehicleError.COMPACT_DELIVERY_ONLY;
        }
        if (photoCount == 0) {
            return VehicleError.PHOTO_REQUIRED;
        }
        return null;
    }

    public Result<Long> register(VehicleForm form, List<Uri> photos, Uri insurance, Uri registration, boolean notSalvage) {
        VehicleError error = validateForm(form, photos.size(), notSalvage);
        if (error == null && (insurance == null || registration == null)) {
            error = VehicleError.DOCUMENTS_REQUIRED;
        }
        if (error != null) {
            return Result.error(error, "Invalid form");
        }
        return vehicleRepository.registerVehicle(sessionManager.getUserId(), form, photos, insurance, registration);
    }

    public Result<Long> restore(String vin) {
        return vehicleRepository.restoreVehicle(sessionManager.getUserId(), vin);
    }

    public Result<Void> update(long vehicleId, VehicleForm form, int photoCount) {
        VehicleError error = validateForm(form, photoCount, true);
        if (error != null) {
            return Result.error(error, "Invalid form");
        }
        return vehicleRepository.updateVehicle(sessionManager.getUserId(), vehicleId, form);
    }

    public Result<Long> addPhoto(long vehicleId, Uri photo) {
        return vehicleRepository.addVehiclePhoto(sessionManager.getUserId(), vehicleId, photo);
    }

    public Result<Void> deletePhoto(long photoId) {
        return vehicleRepository.deleteVehiclePhoto(sessionManager.getUserId(), photoId);
    }

    public List<VehicleDocumentItem> getDocuments(long vehicleId) {
        return documentRepository.getVehicleDocuments(vehicleId);
    }

    public Result<Void> uploadDocument(long vehicleId, DocumentType type, Uri image) {
        return documentRepository.uploadVehicleDocument(sessionManager.getUserId(), vehicleId, type, image);
    }
}
