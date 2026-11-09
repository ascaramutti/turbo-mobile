package com.turbo.mobile.ui.driver.viewmodels;

import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.Calendar;

public class TripViewModel {

    private final PaymentRepository paymentRepository;
    private final SessionManager sessionManager;

    public TripViewModel(PaymentRepository paymentRepository, SessionManager sessionManager) {
        this.paymentRepository = paymentRepository;
        this.sessionManager = sessionManager;
    }

    public boolean isCardNumberValid(String number) {
        if (!number.matches("\\d{16}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 16; i++) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (i % 2 == 0) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }
            sum = sum + digit;
        }
        return sum % 10 == 0;
    }

    public boolean isExpiryValid(String expiry) {
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3));
        if (month < 1 || month > 12) {
            return false;
        }
        Calendar today = Calendar.getInstance();
        int thisYear = today.get(Calendar.YEAR);
        int thisMonth = today.get(Calendar.MONTH) + 1;
        return year > thisYear || (year == thisYear && month >= thisMonth);
    }

    public boolean isCvvValid(String cvv) {
        return cvv.matches("\\d{3}");
    }

    public Result<Void> pay(long bookingId, String cardLast4) {
        return paymentRepository.registerPayment(sessionManager.getUserId(), bookingId, cardLast4);
    }
}
