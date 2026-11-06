package com.turbo.mobile.ui.owner.viewmodels;

import com.turbo.mobile.data.model.OwnerVehicleItem;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleError;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.Calendar;
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

    public long getAvailableUntil(int daysFromToday, int halfHour) {
        Calendar until = Calendar.getInstance();
        until.add(Calendar.DAY_OF_MONTH, daysFromToday);
        until.set(Calendar.HOUR_OF_DAY, halfHour / 2);
        until.set(Calendar.MINUTE, halfHour % 2 * 30);
        until.set(Calendar.SECOND, 0);
        until.set(Calendar.MILLISECOND, 0);
        return until.getTimeInMillis();
    }

    public Result<Void> publish(long vehicleId, long availableUntil) {
        if (availableUntil <= System.currentTimeMillis()) {
            return Result.error(VehicleError.DATE_NOT_FUTURE, "Date is not in the future");
        }
        return vehicleRepository.publish(sessionManager.getUserId(), vehicleId, availableUntil);
    }

    public Result<Void> unpublish(long vehicleId) {
        return vehicleRepository.unpublish(sessionManager.getUserId(), vehicleId);
    }

    public Result<Integer> deleteVehicle(long vehicleId) {
        return vehicleRepository.deleteVehicle(sessionManager.getUserId(), vehicleId);
    }
}
