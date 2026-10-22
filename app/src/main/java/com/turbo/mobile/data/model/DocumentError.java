package com.turbo.mobile.data.model;

public enum DocumentError implements RepoError {
    ACTION_NOT_ALLOWED,
    EXPIRY_NOT_FUTURE,
    NOT_PENDING,
    REASON_TOO_SHORT,
    VEHICLE_DELETED,
    FILE_ERROR,
    NOT_FOUND,
    NOT_ALLOWED,
    UNEXPECTED
}
