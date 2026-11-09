package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.PaymentEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.EarningItem;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.PlatformEarnings;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.DateUtils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RoomPaymentRepository implements PaymentRepository {

    private static final long MINUTE = 60 * 1000;
    private static final long HOUR = 60 * MINUTE;
    private static final double FEE_RATE = 0.20;

    private final AppDatabase db;

    public RoomPaymentRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    @Override
    public Result<Void> registerPayment(long driverId, long bookingId, String cardLast4) {
        try {
            BookingEntity booking = db.bookingDao().findById(bookingId);
            if (booking == null) {
                return Result.error(PaymentError.NOT_FOUND, "Booking " + bookingId + " not found");
            }
            if (booking.driverId != driverId) {
                return Result.error(PaymentError.NOT_ALLOWED, "Only the driver of the booking can pay it");
            }
            if (booking.status != BookingStatus.CONFIRMED) {
                return Result.error(PaymentError.NOT_CONFIRMED, "Only confirmed bookings can be paid.");
            }
            if (db.paymentDao().findByBooking(bookingId) != null) {
                return Result.error(PaymentError.ALREADY_PAID, "This booking is already paid.");
            }
            long now = System.currentTimeMillis();
            if (now >= booking.startTime - 15 * MINUTE) {
                return Result.error(PaymentError.PAYMENT_DEADLINE_PASSED, "The payment deadline has passed.");
            }
            if (!isValidCard(cardLast4)) {
                return Result.error(PaymentError.INVALID_CARD, "Invalid card.");
            }

            PaymentEntity payment = new PaymentEntity();
            payment.bookingId = bookingId;
            payment.amountCents = booking.totalPriceCents;
            payment.platformFeeCents = (int) Math.round(booking.totalPriceCents * FEE_RATE);
            payment.cardLast4 = cardLast4;
            payment.paidAt = now;
            db.paymentDao().insert(payment);
            return Result.success(null);
        } catch (RuntimeException e) {
            Log.e("RoomPaymentRepository", "Unexpected database error", e);
            return Result.error(PaymentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<DriverSpending> getDriverSpending(long driverId) {
        try {
            if (db.userDao().findById(driverId) == null) {
                return Result.error(PaymentError.NOT_FOUND, "Driver " + driverId + " not found");
            }
            DateUtils.Range month = DateUtils.monthRange(System.currentTimeMillis());
            DriverSpending spending = new DriverSpending();
            for (PaymentEntity payment : db.paymentDao().getByDriver(driverId)) {
                spending.setTotalCents(spending.getTotalCents() + payment.amountCents);
                if (month.contains(payment.paidAt)) {
                    spending.setMonthCents(spending.getMonthCents() + payment.amountCents);
                }
            }
            for (BookingEntity booking : db.bookingDao().getByDriver(driverId)) {
                if (booking.status == BookingStatus.COMPLETED) {
                    spending.setCompletedCount(spending.getCompletedCount() + 1);
                }
            }
            return Result.success(spending);
        } catch (RuntimeException e) {
            Log.e("RoomPaymentRepository", "Unexpected database error", e);
            return Result.error(PaymentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<OwnerEarnings> getOwnerEarnings(long ownerId, Long vehicleId) {
        try {
            if (db.userDao().findById(ownerId) == null) {
                return Result.error(PaymentError.NOT_FOUND, "Owner " + ownerId + " not found");
            }
            if (vehicleId != null) {
                VehicleEntity vehicle = db.vehicleDao().findById(vehicleId);
                if (vehicle == null) {
                    return Result.error(PaymentError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
                }
                if (vehicle.ownerId != ownerId) {
                    return Result.error(PaymentError.NOT_ALLOWED, "The vehicle belongs to another owner");
                }
            }

            OwnerEarnings earnings = new OwnerEarnings();
            for (BookingEntity booking : db.bookingDao().getByOwner(ownerId)) {
                if ((vehicleId == null || booking.vehicleId == vehicleId)
                        && booking.status == BookingStatus.COMPLETED) {
                    earnings.setCompletedRentals(earnings.getCompletedRentals() + 1);
                }
            }

            DateUtils.Range month = DateUtils.monthRange(System.currentTimeMillis());
            List<EarningItem> history = new ArrayList<>();
            for (PaymentEntity payment : db.paymentDao().getAll()) {
                BookingEntity booking = db.bookingDao().findById(payment.bookingId);
                VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
                if (vehicle.ownerId != ownerId || (vehicleId != null && vehicle.id != vehicleId)) {
                    continue;
                }
                int payout = payment.amountCents - payment.platformFeeCents;
                earnings.setTotalCents(earnings.getTotalCents() + payout);
                if (month.contains(payment.paidAt)) {
                    earnings.setMonthCents(earnings.getMonthCents() + payout);
                }
                EarningItem item = new EarningItem();
                item.setBookingId(booking.id);
                item.setVehicleTitle(vehicle.make + " " + vehicle.model + " " + vehicle.year);
                item.setDriverName(shortName(booking.driverId));
                item.setPaidAt(payment.paidAt);
                item.setHours((int) ((booking.endTime - booking.startTime) / HOUR));
                item.setPayoutCents(payout);
                history.add(item);
            }
            earnings.setHistory(history);
            return Result.success(earnings);
        } catch (RuntimeException e) {
            Log.e("RoomPaymentRepository", "Unexpected database error", e);
            return Result.error(PaymentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public Result<PlatformEarnings> getPlatformEarnings(YearMonth month) {
        try {
            DateUtils.Range range = DateUtils.monthRange(month);
            PlatformEarnings earnings = new PlatformEarnings();
            earnings.setMonth(month);
            for (PaymentEntity payment : db.paymentDao().getAll()) {
                if (range.contains(payment.paidAt)) {
                    earnings.setFeesCents(earnings.getFeesCents() + payment.platformFeeCents);
                    earnings.setPaymentsCents(earnings.getPaymentsCents() + payment.amountCents);
                }
            }
            for (BookingEntity booking : db.bookingDao().getAll()) {
                if (booking.status == BookingStatus.COMPLETED && booking.completedAt != null
                        && range.contains(booking.completedAt)) {
                    earnings.setCompletedCount(earnings.getCompletedCount() + 1);
                }
            }
            return Result.success(earnings);
        } catch (RuntimeException e) {
            Log.e("RoomPaymentRepository", "Unexpected database error", e);
            return Result.error(PaymentError.UNEXPECTED, e.getMessage());
        }
    }

    @Override
    public List<YearMonth> getPaymentMonths() {
        List<YearMonth> months = new ArrayList<>();
        months.add(DateUtils.yearMonth(System.currentTimeMillis()));
        for (PaymentEntity payment : db.paymentDao().getAll()) {
            YearMonth month = DateUtils.yearMonth(payment.paidAt);
            if (!months.contains(month)) {
                months.add(month);
            }
        }
        return months;
    }

    private boolean isValidCard(String cardLast4) {
        if (cardLast4 == null || cardLast4.length() != 4) {
            return false;
        }
        for (int i = 0; i < cardLast4.length(); i++) {
            if (!Character.isDigit(cardLast4.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String shortName(long userId) {
        UserEntity user = db.userDao().findById(userId);
        return user.firstName + " " + user.lastName.charAt(0) + ".";
    }
}
