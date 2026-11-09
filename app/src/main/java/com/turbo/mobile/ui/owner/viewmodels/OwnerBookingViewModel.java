package com.turbo.mobile.ui.owner.viewmodels;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class OwnerBookingViewModel {

    private final BookingRepository bookingRepository;
    private final SessionManager sessionManager;

    public OwnerBookingViewModel(BookingRepository bookingRepository, SessionManager sessionManager) {
        this.bookingRepository = bookingRepository;
        this.sessionManager = sessionManager;
    }

    public List<BookingListItem> getPendingRequests() {
        return bookingRepository.getPendingByOwner(sessionManager.getUserId());
    }

    public List<BookingListItem> getBookings(OwnerTab tab) {
        return bookingRepository.getOwnerBookings(sessionManager.getUserId(), tab);
    }

    public Result<ConfirmResult> confirm(long bookingId) {
        return bookingRepository.confirm(sessionManager.getUserId(), bookingId);
    }

    public Result<Void> reject(long bookingId, String reason) {
        if (reason.isEmpty()) {
            return Result.error(BookingError.REASON_REQUIRED, "Reason is empty");
        }
        return bookingRepository.reject(sessionManager.getUserId(), bookingId, reason);
    }

    public int getErrorMessage(RepoError code) {
        if (code == BookingError.REASON_REQUIRED) {
            return R.string.booking_requests_error_reason;
        } else if (code == BookingError.NOT_PENDING) {
            return R.string.booking_requests_error_not_pending;
        } else if (code == BookingError.PAYMENT_DEADLINE_PASSED) {
            return R.string.booking_requests_error_too_late;
        } else if (code == BookingError.DRIVER_NOT_VERIFIED) {
            return R.string.booking_requests_error_driver_not_verified;
        } else if (code == BookingError.DOCUMENTS_EXPIRE) {
            return R.string.booking_requests_error_documents_expire;
        } else if (code == BookingError.DRIVER_INACTIVE) {
            return R.string.booking_requests_error_driver_inactive;
        } else if (code == BookingError.VEHICLE_NOT_APPROVED) {
            return R.string.booking_requests_error_vehicle_not_approved;
        } else if (code == BookingError.VEHICLE_DELETED) {
            return R.string.booking_requests_error_vehicle_deleted;
        } else if (code == BookingError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == BookingError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
