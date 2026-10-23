package com.turbo.mobile.data.repository.fake;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.EarningItem;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.PaymentError;
import com.turbo.mobile.data.model.PlatformEarnings;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.util.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeSet;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKINGS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.PAYMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getInt;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLongOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakePaymentRepository implements PaymentRepository {

    private static final long HOUR = 3_600_000L;
    private static final double FEE_RATE = 0.20;

    private final FakeDataStore store;

    public FakePaymentRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public Result<Void> registerPayment(long driverId, long bookingId, String cardLast4) {
        RepoError forced = forced(PaymentError.NOT_CONFIRMED, PaymentError.ALREADY_PAID,
                PaymentError.PAYMENT_DEADLINE_PASSED, PaymentError.INVALID_CARD, PaymentError.NOT_FOUND,
                PaymentError.NOT_ALLOWED, PaymentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(PaymentError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        int amount = getInt(booking, "total_price_cents");
        JSONObject payment = new JSONObject();
        set(payment, "booking_id", bookingId);
        set(payment, "amount_cents", amount);
        set(payment, "platform_fee_cents", (int) Math.round(amount * FEE_RATE));
        set(payment, "card_last4", cardLast4);
        set(payment, "paid_at", store.now());
        store.insert(PAYMENTS, payment);
        set(booking, "_paid", true);
        set(booking, "_driver_actions", new JSONArray());
        return Result.success(null);
    }

    @Override
    public Result<DriverSpending> getDriverSpending(long driverId) {
        RepoError forced = forced(PaymentError.NOT_FOUND, PaymentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(USERS, driverId) == null) {
            return Result.error(PaymentError.NOT_FOUND, "Driver " + driverId + " not found");
        }
        DateUtils.Range month = DateUtils.monthRange(store.now());
        DriverSpending spending = new DriverSpending();
        for (JSONObject booking : store.where(BOOKINGS, "driver_id", driverId)) {
            JSONObject payment = paymentOf(getLong(booking, "id"));
            if (payment != null) {
                int amount = getInt(payment, "amount_cents");
                spending.setTotalCents(spending.getTotalCents() + amount);
                if (month.contains(getLong(payment, "paid_at"))) {
                    spending.setMonthCents(spending.getMonthCents() + amount);
                }
            }
            if (isCompleted(booking)) {
                spending.setCompletedCount(spending.getCompletedCount() + 1);
            }
        }
        return Result.success(spending);
    }

    @Override
    public Result<OwnerEarnings> getOwnerEarnings(long ownerId, @Nullable Long vehicleId) {
        RepoError forced = forced(PaymentError.NOT_FOUND, PaymentError.NOT_ALLOWED, PaymentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(USERS, ownerId) == null) {
            return Result.error(PaymentError.NOT_FOUND, "Owner " + ownerId + " not found");
        }
        if (vehicleId != null && store.find(VEHICLES, vehicleId) == null) {
            return Result.error(PaymentError.NOT_FOUND, "Vehicle " + vehicleId + " not found");
        }
        DateUtils.Range month = DateUtils.monthRange(store.now());
        OwnerEarnings earnings = new OwnerEarnings();
        List<EarningItem> history = new ArrayList<>();
        for (JSONObject vehicle : store.where(VEHICLES, "owner_id", ownerId)) {
            if (vehicleId != null && getLong(vehicle, "id") != vehicleId) {
                continue;
            }
            for (JSONObject booking : store.where(BOOKINGS, "vehicle_id", getLong(vehicle, "id"))) {
                if (isCompleted(booking)) {
                    earnings.setCompletedRentals(earnings.getCompletedRentals() + 1);
                }
                JSONObject payment = paymentOf(getLong(booking, "id"));
                if (payment == null) {
                    continue;
                }
                int payout = getInt(payment, "amount_cents") - getInt(payment, "platform_fee_cents");
                long paidAt = getLong(payment, "paid_at");
                earnings.setTotalCents(earnings.getTotalCents() + payout);
                if (month.contains(paidAt)) {
                    earnings.setMonthCents(earnings.getMonthCents() + payout);
                }
                EarningItem item = new EarningItem();
                item.setBookingId(getLong(booking, "id"));
                item.setVehicleTitle(FakeVehicles.title(vehicle));
                item.setDriverName(FakeVehicles.shortName(store.find(USERS, getLong(booking, "driver_id"))));
                item.setPaidAt(paidAt);
                item.setHours((int) ((getLong(booking, "end_time") - getLong(booking, "start_time")) / HOUR));
                item.setPayoutCents(payout);
                history.add(item);
            }
        }
        history.sort((a, b) -> a.getPaidAt() != b.getPaidAt()
                ? Long.compare(b.getPaidAt(), a.getPaidAt())
                : Long.compare(b.getBookingId(), a.getBookingId()));
        earnings.setHistory(history);
        return Result.success(earnings);
    }

    @Override
    public Result<PlatformEarnings> getPlatformEarnings(YearMonth month) {
        RepoError forced = forced(PaymentError.NOT_ALLOWED, PaymentError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        DateUtils.Range range = DateUtils.monthRange(month);
        PlatformEarnings earnings = new PlatformEarnings();
        earnings.setMonth(month);
        for (JSONObject payment : store.rows(PAYMENTS)) {
            if (range.contains(getLong(payment, "paid_at"))) {
                earnings.setFeesCents(earnings.getFeesCents() + getInt(payment, "platform_fee_cents"));
                earnings.setPaymentsCents(earnings.getPaymentsCents() + getInt(payment, "amount_cents"));
            }
        }
        for (JSONObject booking : store.rows(BOOKINGS)) {
            Long completedAt = getLongOrNull(booking, "completed_at");
            if (isCompleted(booking) && completedAt != null && range.contains(completedAt)) {
                earnings.setCompletedCount(earnings.getCompletedCount() + 1);
            }
        }
        return Result.success(earnings);
    }

    @Override
    public List<YearMonth> getPaymentMonths() {
        TreeSet<YearMonth> months = new TreeSet<>();
        months.add(DateUtils.yearMonth(store.now()));
        for (JSONObject payment : store.rows(PAYMENTS)) {
            months.add(DateUtils.yearMonth(getLong(payment, "paid_at")));
        }
        return new ArrayList<>(months.descendingSet());
    }

    private JSONObject paymentOf(long bookingId) {
        List<JSONObject> payments = store.where(PAYMENTS, "booking_id", bookingId);
        return payments.isEmpty() ? null : payments.get(0);
    }

    private static boolean isCompleted(JSONObject booking) {
        return BookingStatus.COMPLETED.name().equals(getString(booking, "status"));
    }

    private static RepoError forced(PaymentError first, PaymentError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
