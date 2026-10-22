package com.turbo.mobile.data.repository;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.data.model.WeeklyHours;

import java.util.List;

public interface BookingRepository {

    Result<Long> createBooking(long driverId, long vehicleId, long start, long end, ServiceType serviceType);

    List<BookingListItem> getDriverBookings(long driverId, DriverTab tab);

    Result<WeeklyHours> getWeeklyHours(long driverId);

    Result<BookingDetail> getBookingDetail(long viewerId, long bookingId);

    Result<Void> cancelBooking(long driverId, long bookingId);

    Result<Void> canStart(long driverId, long bookingId);

    Result<Void> startTrip(long driverId, long bookingId, List<Uri> pickupPhotos);

    Result<Void> endTrip(long driverId, long bookingId, List<Uri> returnPhotos);

    List<BookingListItem> getPendingByOwner(long ownerId);

    List<BookingListItem> getOwnerBookings(long ownerId, OwnerTab tab);

    Result<ConfirmResult> confirm(long ownerId, long bookingId);

    Result<Void> reject(long ownerId, long bookingId, String reason);

    List<BookingListItem> getAllBookings(@Nullable BookingStatus status);

    Result<InspectionPhotos> getInspectionPhotos(long viewerId, long bookingId);
}
