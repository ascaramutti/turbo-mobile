package com.turbo.mobile.ui.driver.viewmodels;

import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class MyBookingsViewModel {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SessionManager sessionManager;

    public MyBookingsViewModel(BookingRepository bookingRepository, PaymentRepository paymentRepository, SessionManager sessionManager) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.sessionManager = sessionManager;
    }

    public List<BookingListItem> getBookings(DriverTab tab) {
        return bookingRepository.getDriverBookings(sessionManager.getUserId(), tab);
    }

    public Result<WeeklyHours> loadWeeklyHours() {
        return bookingRepository.getWeeklyHours(sessionManager.getUserId());
    }

    public Result<DriverSpending> loadSpending() {
        return paymentRepository.getDriverSpending(sessionManager.getUserId());
    }
}
