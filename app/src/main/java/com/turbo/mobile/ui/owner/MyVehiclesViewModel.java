package com.turbo.mobile.ui.owner;

import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class MyVehiclesViewModel {

    private final VehicleRepository vehicleRepository;
    private final SessionManager sessionManager;

    public MyVehiclesViewModel(VehicleRepository vehicleRepository, SessionManager sessionManager) {
        this.vehicleRepository = vehicleRepository;
        this.sessionManager = sessionManager;
    }

    public List<OwnerVehicleItem> getVehicles() {
        return vehicleRepository.getOwnerVehicles(sessionManager.getUserId());
    }

    public Result<Void> unpublish(long vehicleId) {
        return vehicleRepository.unpublish(sessionManager.getUserId(), vehicleId);
    }

    public Result<Integer> deleteVehicle(long vehicleId) {
        return vehicleRepository.deleteVehicle(sessionManager.getUserId(), vehicleId);
    }
}
