package com.turbo.mobile.data.repository.room;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.db.entity.DriverProfileEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DriverState;

final class DriverStates {

    static final long RENEW_WINDOW = 30L * 24 * 60 * 60 * 1000;

    private DriverStates() {
    }

    static DriverState of(AppDatabase db, UserEntity driver, long now) {
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
        boolean renew = profile != null
                && Math.min(profile.licenseExpiry, profile.studyPermitExpiry) <= now + RENEW_WINDOW;
        return renew ? DriverState.VERIFIED_RENEW : DriverState.VERIFIED;
    }
}
