package com.turbo.mobile.data.repository;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.DriverSpending;
import com.turbo.mobile.data.model.OwnerEarnings;
import com.turbo.mobile.data.model.PlatformEarnings;
import com.turbo.mobile.data.model.Result;

import java.time.YearMonth;
import java.util.List;

public interface PaymentRepository {

    Result<Void> registerPayment(long driverId, long bookingId, String cardLast4);

    Result<DriverSpending> getDriverSpending(long driverId);

    Result<OwnerEarnings> getOwnerEarnings(long ownerId, @Nullable Long vehicleId);

    Result<PlatformEarnings> getPlatformEarnings(YearMonth month);

    List<YearMonth> getPaymentMonths();
}
