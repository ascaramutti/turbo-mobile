package com.turbo.mobile.ui.admin.viewmodels;

import androidx.annotation.Nullable;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.PlatformEarnings;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.SessionManager;

import java.time.YearMonth;
import java.util.List;

/**
 * ViewModel for Admin all bookings, platform earnings, and inspection photos screens.
 */
public class AllBookingsViewModel {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SessionManager sessionManager;

    public AllBookingsViewModel(BookingRepository bookingRepository, PaymentRepository paymentRepository, SessionManager sessionManager) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.sessionManager = sessionManager;
    }

    public List<YearMonth> getPaymentMonths() {
        return paymentRepository.getPaymentMonths();
    }

    public Result<PlatformEarnings> getPlatformEarnings(YearMonth month) {
        return paymentRepository.getPlatformEarnings(month);
    }

    public List<BookingListItem> getBookings(@Nullable BookingStatus status) {
        return bookingRepository.getAllBookings(status);
    }

    public Result<InspectionPhotos> getInspectionPhotos(long bookingId) {
        return bookingRepository.getInspectionPhotos(sessionManager.getUserId(), bookingId);
    }

    public int getErrorMessage(RepoError code) {
        if (code == BookingError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == BookingError.NOT_ALLOWED || code == PaymentError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
