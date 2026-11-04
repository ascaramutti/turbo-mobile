package com.turbo.mobile.data.repository.room;

import android.content.Context;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.db.entity.DriverProfileEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DriverState;

public class DriverStateCalculator {

    private final AppDatabase db;

    public DriverStateCalculator(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public DriverState calculate(UserEntity driver) {
        if (!driver.isActive) {
            return DriverState.DEACTIVATED;
        }
        int approved = 0;
        for (DriverDocumentEntity document : db.driverDocumentDao().getAll(driver.id)) {
            if (document.status == DocumentStatus.REJECTED) {
                return DriverState.DOCUMENT_REJECTED;
            }
            if (document.status == DocumentStatus.APPROVED) {
                approved++;
            }
        }
        if (approved < 2) {
            return DriverState.PENDING_VERIFICATION;
        }
        DriverProfileEntity profile = db.driverProfileDao().findByUserId(driver.id);
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        if (profile != null && Math.min(profile.licenseExpiry, profile.studyPermitExpiry) <= System.currentTimeMillis() + thirtyDays) {
            return DriverState.VERIFIED_RENEW;
        }
        return DriverState.VERIFIED;
    }
}
