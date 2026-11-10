package com.turbo.mobile.ui.driver.viewmodels;

import android.net.Uri;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.Calendar;
import java.util.List;

public class TripViewModel {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SessionManager sessionManager;

    public TripViewModel(BookingRepository bookingRepository, PaymentRepository paymentRepository, SessionManager sessionManager) {
        this.bookingRepository = bookingRepository;
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

    public Result<Void> canStart(long bookingId) {
        return bookingRepository.canStart(sessionManager.getUserId(), bookingId);
    }

    public Result<Void> startTrip(long bookingId, List<Uri> pickupPhotos) {
        if (pickupPhotos.isEmpty()) {
            return Result.error(BookingError.PHOTO_COUNT, "No photos");
        }
        return bookingRepository.startTrip(sessionManager.getUserId(), bookingId, pickupPhotos);
    }

    public Result<Void> endTrip(long bookingId, List<Uri> returnPhotos) {
        if (returnPhotos.isEmpty()) {
            return Result.error(BookingError.PHOTO_COUNT, "No photos");
        }
        return bookingRepository.endTrip(sessionManager.getUserId(), bookingId, returnPhotos);
    }

    public int getErrorMessage(RepoError code, boolean isReturn) {
        if (code == BookingError.PHOTO_COUNT) {
            return R.string.trip_photos_error_photo_count;
        } else if (code == BookingError.NOT_PAID) {
            return R.string.trip_photos_error_not_paid;
        } else if (code == BookingError.OUTSIDE_START_WINDOW) {
            return R.string.trip_photos_error_start_window;
        } else if (code == BookingError.WRONG_STATUS && isReturn) {
            return R.string.trip_photos_error_not_in_progress;
        } else if (code == BookingError.WRONG_STATUS) {
            return R.string.trip_photos_error_cannot_start;
        } else if (code == BookingError.FILE_ERROR) {
            return R.string.trip_photos_error_file;
        } else if (code == BookingError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == BookingError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
