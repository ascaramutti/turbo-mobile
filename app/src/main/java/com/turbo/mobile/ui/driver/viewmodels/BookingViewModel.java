package com.turbo.mobile.ui.driver.viewmodels;

import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.VehicleDetail;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BookingViewModel {

    private static final long ONE_HOUR = 60 * 60 * 1000L;
    private static final int DAYS_AHEAD = 14;

    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final SessionManager sessionManager;

    public BookingViewModel(VehicleRepository vehicleRepository, DocumentRepository documentRepository, SessionManager sessionManager) {
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.sessionManager = sessionManager;
    }

    public Result<VehicleDetail> loadVehicle(long vehicleId) {
        return vehicleRepository.getVehicleDetail(vehicleId);
    }

    public Result<DriverStatus> loadDriverStatus() {
        return documentRepository.getDriverStatus(sessionManager.getUserId());
    }

    public int computeTotalCents(int hours, int hourlyRateCents) {
        return hours * hourlyRateCents;
    }

    public List<Long> getShiftDays(Long availableUntil) {
        List<Long> days = new ArrayList<>();
        if (availableUntil == null) {
            return days;
        }
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < DAYS_AHEAD && day.getTimeInMillis() < availableUntil; i++) {
            days.add(day.getTimeInMillis());
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    public long getShiftStart(long day, int halfHour) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(day);
        start.set(Calendar.HOUR_OF_DAY, halfHour / 2);
        start.set(Calendar.MINUTE, halfHour % 2 * 30);
        return start.getTimeInMillis();
    }

    public boolean startsTooSoon(long start) {
        return start < System.currentTimeMillis() + 2 * ONE_HOUR;
    }

    public boolean endsAfterAvailable(long start, int hours, long availableUntil) {
        return start + hours * ONE_HOUR > availableUntil;
    }
}
