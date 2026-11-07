package com.turbo.mobile.data.repository.room;

import android.content.Context;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.StatusReasons;

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
                BookingListItem item = toListItem(booking);
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

    private BookingListItem toListItem(BookingEntity booking) {
        VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
        BookingListItem item = new BookingListItem();
        item.setId(booking.id);
        item.setVehicleId(vehicle.id);
        item.setVehicleTitle(title(vehicle));
        item.setThumbnailPath(thumbnail(vehicle.id));
        item.setDriverName(shortName(booking.driverId));
        item.setOwnerName(shortName(vehicle.ownerId));
        item.setCounterpartRatingAvg(db.reviewDao().getRatingSummary(vehicle.ownerId).getAverage());
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
        return item;
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
