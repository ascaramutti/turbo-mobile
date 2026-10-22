package com.turbo.mobile.data.model;

public enum ReviewError implements RepoError {
    NOT_COMPLETED,
    ALREADY_REVIEWED,
    INVALID_RATING,
    NOT_FOUND,
    NOT_ALLOWED,
    UNEXPECTED
}
