package com.turbo.mobile.data.model;

public enum PaymentError implements RepoError {
    NOT_CONFIRMED,
    ALREADY_PAID,
    PAYMENT_DEADLINE_PASSED,
    INVALID_CARD,
    CARD_DECLINED,
    NOT_FOUND,
    NOT_ALLOWED,
    UNEXPECTED
}
