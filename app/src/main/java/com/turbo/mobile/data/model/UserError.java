package com.turbo.mobile.data.model;

public enum UserError implements RepoError {
    INVALID_CREDENTIALS,
    ACCOUNT_DEACTIVATED,
    EMAIL_TAKEN,
    LICENSE_NUMBER_TAKEN,
    UNDER_18,
    EXPIRY_NOT_FUTURE,
    INVALID_INPUT,
    CANNOT_CHANGE_ADMIN,
    FILE_ERROR,
    NOT_FOUND,
    NOT_ALLOWED,
    UNEXPECTED
}
