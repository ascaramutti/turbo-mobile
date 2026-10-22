package com.turbo.mobile.data.model;

public class DriverStatus {

    private DriverState state;
    private boolean verified;
    private boolean canBook;
    private LicenseClass licenseClass;
    private long licenseExpiry;
    private long studyPermitExpiry;

    public DriverState getState() {
        return state;
    }

    public void setState(DriverState state) {
        this.state = state;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean canBook() {
        return canBook;
    }

    public void setCanBook(boolean canBook) {
        this.canBook = canBook;
    }

    public LicenseClass getLicenseClass() {
        return licenseClass;
    }

    public void setLicenseClass(LicenseClass licenseClass) {
        this.licenseClass = licenseClass;
    }

    public long getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(long licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public long getStudyPermitExpiry() {
        return studyPermitExpiry;
    }

    public void setStudyPermitExpiry(long studyPermitExpiry) {
        this.studyPermitExpiry = studyPermitExpiry;
    }
}
