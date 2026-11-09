package com.turbo.mobile.ui.driver.viewmodels;

import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewItem;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.ReviewRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class BookingDetailViewModel {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final SessionManager sessionManager;

    public BookingDetailViewModel(BookingRepository bookingRepository, ReviewRepository reviewRepository, SessionManager sessionManager) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.sessionManager = sessionManager;
    }

    public Result<BookingDetail> load(long bookingId) {
        return bookingRepository.getBookingDetail(sessionManager.getUserId(), bookingId);
    }

    public boolean isOwner(BookingDetail booking) {
        return booking.getOwnerId() == sessionManager.getUserId();
    }

    public Result<Void> cancel(long bookingId) {
        return bookingRepository.cancelBooking(sessionManager.getUserId(), bookingId);
    }

    public List<ReviewItem> getReviews(long bookingId) {
        return reviewRepository.getReviewsForBooking(bookingId);
    }
}
