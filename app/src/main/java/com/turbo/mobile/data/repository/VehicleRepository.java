package com.turbo.mobile.data.repository;

import android.net.Uri;

import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.SearchFilter;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.model.VehicleEditData;
import com.turbo.mobile.data.model.VehicleForm;
import com.turbo.mobile.data.model.VehicleSummary;
import com.turbo.mobile.data.model.Zone;

import java.util.List;

public interface VehicleRepository {

    List<Zone> getZones();

    List<VehicleSummary> searchPublished(SearchFilter filter);

    Result<VehicleDetail> getVehicleDetail(long vehicleId);

    List<OwnerVehicleItem> getOwnerVehicles(long ownerId);

    Result<Long> registerVehicle(long ownerId, VehicleForm form, List<Uri> photos, Uri insurance, Uri registration);

    Result<Long> restoreVehicle(long ownerId, String vin);

    Result<VehicleEditData> getVehicleForEdit(long ownerId, long vehicleId);

    Result<Void> updateVehicle(long ownerId, long vehicleId, VehicleForm form);

    Result<Long> addVehiclePhoto(long ownerId, long vehicleId, Uri photo);

    Result<Void> deleteVehiclePhoto(long ownerId, long photoId);

    Result<Void> publish(long ownerId, long vehicleId, long availableUntil);

    Result<Void> unpublish(long ownerId, long vehicleId);

    Result<Integer> deleteVehicle(long ownerId, long vehicleId);
}
