package com.turbo.mobile.data.repository.room;

import android.content.Context;
import android.util.Log;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.BookingPhotoEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.PaymentEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.PhotoType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class RoomBookingRepository {

    private static final long MINUTE = 60 * 1000;
    private static final long HOUR = 60 * MINUTE;

    private final AppDatabase db;

    public RoomBookingRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public List<BookingListItem> getDriverBookings(long driverId, DriverTab tab) {
        expireBookings();
        List<BookingListItem> items = new ArrayList<>();
        for (BookingEntity booking : db.bookingDao().getByDriver(driverId)) {
            if (tab.getStatuses().contains(booking.status)) {
                BookingListItem item = new BookingListItem();
                fillItem(item, booking);
                item.setCounterpartRatingAvg(ownerRating(booking));
                item.setDriverActions(driverActions(booking));
                if (tab == DriverTab.HISTORY) {
                    items.add(0, item);
                } else {
                    items.add(item);
                }
            }
        }
        return items;
    }

    public Result<WeeklyHours> getWeeklyHours(long driverId) {
        try {
            if (db.userDao().findById(driverId) == null) {
                return Result.error(BookingError.NOT_FOUND, "Driver " + driverId + " not found");
            }
            expireBookings();
            DateUtils.Range week = DateUtils.weekRange(System.currentTimeMillis());
            double hours = 0;
            for (BookingEntity booking : db.bookingDao().getWeekBookings(driverId, week.getStart(), week.getEnd())) {
                hours += (booking.endTime - booking.startTime) / (double) HOUR;
            }
            WeeklyHours weeklyHours = new WeeklyHours();
            weeklyHours.setWeekStart(week.getStart());
            weeklyHours.setWeekEnd(week.getEnd());
            weeklyHours.setBookedHours(hours);
            weeklyHours.setLimitHours(24);
            return Result.success(weeklyHours);
        } catch (RuntimeException e) {
            Log.e("RoomBookingRepository", "Unexpected database error", e);
            return Result.error(BookingError.UNEXPECTED, e.getMessage());
        }
    }

    public Result<BookingDetail> getBookingDetail(long viewerId, long bookingId) {
        try {
            expireBookings();
            BookingEntity booking = db.bookingDao().findById(bookingId);
            if (booking == null) {
                return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
            }
            VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
            boolean isDriver = viewerId == booking.driverId;
            boolean isOwner = viewerId == vehicle.ownerId;
            if (!isDriver && !isOwner && !isAdmin(viewerId)) {
                return Result.error(BookingError.NOT_ALLOWED, "User " + viewerId + " cannot see booking " + bookingId);
            }
            BookingDetail detail = new BookingDetail();
            fillItem(detail, booking);
            detail.setOverlappingPendingIds(new ArrayList<>());
            if (isDriver) {
                detail.setCounterpartRatingAvg(ownerRating(booking));
                detail.setDriverActions(driverActions(booking));
            } else if (isOwner) {
                detail.setCounterpartRatingAvg(db.reviewDao().getRatingSummary(booking.driverId).getAverage());
                detail.setOwnerActions(ownerActions(booking));
                if (booking.status == BookingStatus.PENDING) {
                    detail.setOverlappingPendingIds(db.bookingDao().getOverlappingPendingIds(
                            booking.vehicleId, booking.id, booking.startTime, booking.endTime));
                }
            }
            detail.setPickupAddress(vehicle.address);
            detail.setLatitude(vehicle.latitude);
            detail.setLongitude(vehicle.longitude);
            detail.setDriverId(booking.driverId);
            detail.setOwnerId(vehicle.ownerId);
            detail.setStartedAt(booking.startedAt);
            detail.setCompletedAt(booking.completedAt);
            detail.setCreatedAt(booking.createdAt);
            detail.setPayDeadline(booking.startTime - 15 * MINUTE);
            detail.setStartWindowCloses(booking.startTime + HOUR);
            PaymentEntity payment = db.paymentDao().findByBooking(booking.id);
            if (payment != null) {
                detail.setPaidAt(payment.paidAt);
                detail.setCardLast4(payment.cardLast4);
                if (!isDriver) {
                    detail.setFeeCents(payment.platformFeeCents);
                    detail.setOwnerPayoutCents(payment.amountCents - payment.platformFeeCents);
                }
            }
            detail.setPickupPhotoCount(db.bookingPhotoDao().getAll(booking.id, PhotoType.PICKUP).size());
            detail.setReturnPhotoCount(db.bookingPhotoDao().getAll(booking.id, PhotoType.RETURN).size());
            detail.setReviewedByDriver(db.reviewDao().find(booking.id, booking.driverId) != null);
            detail.setReviewedByOwner(db.reviewDao().find(booking.id, vehicle.ownerId) != null);
            return Result.success(detail);
        } catch (RuntimeException e) {
            Log.e("RoomBookingRepository", "Unexpected database error", e);
            return Result.error(BookingError.UNEXPECTED, e.getMessage());
        }
    }

    public Result<InspectionPhotos> getInspectionPhotos(long viewerId, long bookingId) {
        try {
            if (db.bookingDao().findById(bookingId) == null) {
                return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
            }
            if (!isAdmin(viewerId)) {
                return Result.error(BookingError.NOT_ALLOWED, "Only an Admin can see the inspection photos");
            }
            List<String> pickup = new ArrayList<>();
            for (BookingPhotoEntity photo : db.bookingPhotoDao().getAll(bookingId, PhotoType.PICKUP)) {
                pickup.add(photo.filePath);
            }
            List<String> returned = new ArrayList<>();
            for (BookingPhotoEntity photo : db.bookingPhotoDao().getAll(bookingId, PhotoType.RETURN)) {
                returned.add(photo.filePath);
            }
            InspectionPhotos photos = new InspectionPhotos();
            photos.setBookingId(bookingId);
            photos.setPickupPaths(pickup);
            photos.setReturnPaths(returned);
            return Result.success(photos);
        } catch (RuntimeException e) {
            Log.e("RoomBookingRepository", "Unexpected database error", e);
            return Result.error(BookingError.UNEXPECTED, e.getMessage());
        }
    }

    private void expireBookings() {
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            for (BookingEntity booking : db.bookingDao().getExpiredRequests(now)) {
                close(booking, BookingStatus.REJECTED, StatusReasons.EXPIRED);
                VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
                notify(booking.driverId, "Booking request expired", "Your request for the " + title(vehicle)
                        + " expired. The owner did not confirm in time.");
                notify(vehicle.ownerId, "Booking request expired", fullName(booking.driverId) + "'s request for your "
                        + title(vehicle) + " expired because it was not confirmed in time.");
            }
            for (BookingEntity booking : db.bookingDao().getNotPaid(now)) {
                close(booking, BookingStatus.CANCELLED, StatusReasons.NOT_PAID);
                VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
                notify(booking.driverId, "Booking cancelled", "Your " + title(vehicle)
                        + " booking was cancelled: " + StatusReasons.NOT_PAID + ".");
                notify(vehicle.ownerId, "Booking cancelled", fullName(booking.driverId) + "'s booking for your "
                        + title(vehicle) + " was cancelled: " + StatusReasons.NOT_PAID + ".");
            }
            for (BookingEntity booking : db.bookingDao().getNoShows(now)) {
                close(booking, BookingStatus.CANCELLED, StatusReasons.NO_SHOW);
                VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
                notify(booking.driverId, "Booking cancelled", "Your " + title(vehicle)
                        + " booking was cancelled: " + StatusReasons.NO_SHOW + ". The payment is not refunded.");
                notify(vehicle.ownerId, "Booking cancelled", fullName(booking.driverId) + "'s paid booking for your "
                        + title(vehicle) + " was cancelled: " + StatusReasons.NO_SHOW + ". The payment is kept.");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void fillItem(BookingListItem item, BookingEntity booking) {
        VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
        item.setId(booking.id);
        item.setVehicleId(vehicle.id);
        item.setVehicleTitle(title(vehicle));
        item.setThumbnailPath(thumbnail(vehicle.id));
        item.setDriverName(shortName(booking.driverId));
        item.setOwnerName(shortName(vehicle.ownerId));
        item.setStart(booking.startTime);
        item.setEnd(booking.endTime);
        item.setHours((int) ((booking.endTime - booking.startTime) / HOUR));
        item.setServiceType(booking.serviceType);
        item.setTotalPriceCents(booking.totalPriceCents);
        item.setStatus(booking.status);
        item.setStatusReason(booking.statusReason);
        item.setPaid(isPaid(booking));
        item.setOverdue(booking.status == BookingStatus.IN_PROGRESS && booking.endTime < System.currentTimeMillis());
        item.setDriverActions(new ArrayList<>());
        item.setOwnerActions(new ArrayList<>());
    }

    private Double ownerRating(BookingEntity booking) {
        VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
        return db.reviewDao().getRatingSummary(vehicle.ownerId).getAverage();
    }

    private List<DriverAction> driverActions(BookingEntity booking) {
        List<DriverAction> actions = new ArrayList<>();
        if (!db.userDao().findById(booking.driverId).isActive) {
            return actions;
        }
        long now = System.currentTimeMillis();
        boolean paid = isPaid(booking);
        if (booking.status == BookingStatus.CONFIRMED && !paid && now < booking.startTime - 15 * MINUTE) {
            actions.add(DriverAction.PAY);
        }
        if (booking.status == BookingStatus.PENDING || (booking.status == BookingStatus.CONFIRMED && !paid)) {
            actions.add(DriverAction.CANCEL);
        }
        if (booking.status == BookingStatus.CONFIRMED && paid
                && now >= booking.startTime - 15 * MINUTE && now <= booking.startTime + HOUR) {
            actions.add(DriverAction.START_TRIP);
        }
        if (booking.status == BookingStatus.IN_PROGRESS) {
            actions.add(DriverAction.END_TRIP);
        }
        if (booking.status == BookingStatus.COMPLETED && db.reviewDao().find(booking.id, booking.driverId) == null) {
            actions.add(DriverAction.REVIEW);
        }
        return actions;
    }

    private List<OwnerAction> ownerActions(BookingEntity booking) {
        List<OwnerAction> actions = new ArrayList<>();
        VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
        if (!db.userDao().findById(vehicle.ownerId).isActive) {
            return actions;
        }
        if (booking.status == BookingStatus.PENDING && System.currentTimeMillis() < booking.startTime - 15 * MINUTE) {
            actions.add(OwnerAction.CONFIRM);
            actions.add(OwnerAction.REJECT);
        }
        if (booking.status == BookingStatus.COMPLETED && db.reviewDao().find(booking.id, vehicle.ownerId) == null) {
            actions.add(OwnerAction.REVIEW);
        }
        return actions;
    }

    private boolean isAdmin(long userId) {
        UserEntity user = db.userDao().findById(userId);
        return user != null && user.role == Role.ADMIN;
    }

    private boolean isPaid(BookingEntity booking) {
        return db.paymentDao().findByBooking(booking.id) != null;
    }

    private void close(BookingEntity booking, BookingStatus status, String reason) {
        booking.status = status;
        booking.statusReason = reason;
        db.bookingDao().update(booking);
    }

    private void notify(long userId, String title, String message) {
        NotificationEntity notification = new NotificationEntity();
        notification.userId = userId;
        notification.title = title;
        notification.message = message;
        notification.isRead = false;
        notification.createdAt = System.currentTimeMillis();
        db.notificationDao().insert(notification);
    }

    private String fullName(long userId) {
        UserEntity user = db.userDao().findById(userId);
        return user.firstName + " " + user.lastName;
    }

    private String shortName(long userId) {
        UserEntity user = db.userDao().findById(userId);
        return user.firstName + " " + user.lastName.charAt(0) + ".";
    }

    private String title(VehicleEntity vehicle) {
        return vehicle.make + " " + vehicle.model + " " + vehicle.year;
    }

    private String thumbnail(long vehicleId) {
        List<VehiclePhotoEntity> photos = db.vehiclePhotoDao().getAll(vehicleId);
        if (photos.isEmpty()) {
            return null;
        }
        return photos.get(0).filePath;
    }
}
