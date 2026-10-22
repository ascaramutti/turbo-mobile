package com.turbo.mobile.data.model;

public class RegistrationForm {

    private Role role;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private long dateOfBirth;
    private boolean acceptedTerms;
    private String licenseNumber;
    private LicenseClass licenseClass;
    private long licenseExpiry;
    private String studyPermitNumber;
    private long studyPermitExpiry;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(long dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
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

    public String getStudyPermitNumber() {
        return studyPermitNumber;
    }

    public void setStudyPermitNumber(String studyPermitNumber) {
        this.studyPermitNumber = studyPermitNumber;
    }

    public long getStudyPermitExpiry() {
        return studyPermitExpiry;
    }

    public void setStudyPermitExpiry(long studyPermitExpiry) {
        this.studyPermitExpiry = studyPermitExpiry;
    }
}
