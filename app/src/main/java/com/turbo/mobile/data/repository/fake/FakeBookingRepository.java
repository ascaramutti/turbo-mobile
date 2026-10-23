package com.turbo.mobile.data.repository.fake;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.DriverTab;
import com.turbo.mobile.data.model.InspectionPhotos;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.OwnerTab;
import com.turbo.mobile.data.model.PhotoType;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.data.model.WeeklyHours;
import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.util.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKINGS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKING_PHOTOS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.PAYMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.REVIEWS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getBoolean;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getDoubleOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getInt;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLongList;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLongOrNull;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getStringList;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeBookingRepository implements BookingRepository {

    private static final long HOUR = 3_600_000L;
    private static final long MINUTE = 60_000L;
    private static final int WEEKLY_LIMIT_HOURS = 24;
    private static final List<BookingStatus> WEEKLY_STATUSES = Arrays.asList(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED);

    private enum Viewer {
        DRIVER,
        OWNER,
        ADMIN
    }

    private final FakeDataStore store;

    public FakeBookingRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public Result<Long> createBooking(long driverId, long vehicleId, long start, long end, ServiceType serviceType) {
        RepoError forced = forced(BookingError.DRIVER_NOT_VERIFIED, BookingError.DRIVER_INACTIVE,
                BookingError.DOCUMENTS_EXPIRE, BookingError.VEHICLE_NOT_PUBLISHED, BookingError.AFTER_AVAILABLE_UNTIL,
                BookingError.START_TOO_SOON, BookingError.START_TOO_FAR, BookingError.INVALID_START_STEP,
                BookingError.INVALID_DURATION, BookingError.TAXI_NEEDS_CLASS_4, BookingError.TAXI_NOT_OFFERED,
                BookingError.VEHICLE_OVERLAP, BookingError.DRIVER_OVERLAP, BookingError.WEEKLY_LIMIT,
                BookingError.NOT_FOUND, BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject driver = store.find(USERS, driverId);
        JSONObject vehicle = store.find(VEHICLES, vehicleId);
        if (driver == null || vehicle == null) {
            return Result.error(BookingError.NOT_FOUND, "Driver " + driverId + " or vehicle " + vehicleId + " not found");
        }
        int hours = (int) ((end - start) / HOUR);
        JSONObject booking = new JSONObject();
        set(booking, "driver_id", driverId);
        set(booking, "vehicle_id", vehicleId);
        set(booking, "start_time", start);
        set(booking, "end_time", end);
        set(booking, "total_price_cents", hours * getInt(vehicle, "hourly_rate_cents"));
        set(booking, "status", BookingStatus.PENDING.name());
        set(booking, "service_type", serviceType.name());
        set(booking, "status_reason", null);
        set(booking, "started_at", null);
        set(booking, "completed_at", null);
        set(booking, "created_at", store.now());
        set(booking, "_paid", false);
        set(booking, "_overdue", false);
        set(booking, "_driver_actions", new JSONArray(Collections.singletonList(DriverAction.CANCEL.name())));
        set(booking, "_owner_actions", new JSONArray(Arrays.asList(OwnerAction.CONFIRM.name(), OwnerAction.REJECT.name())));
        set(booking, "_overlapping_pending_ids", new JSONArray());
        long bookingId = store.insert(BOOKINGS, booking);

        FakeNotifications.add(store, getLong(vehicle, "owner_id"), "New booking request",
                fullName(driver) + " requested your " + FakeVehicles.title(vehicle) + " (" + shift(booking) + ").");
        return Result.success(bookingId);
    }

    @Override
    public List<BookingListItem> getDriverBookings(long driverId, DriverTab tab) {
        List<BookingListItem> result = new ArrayList<>();
        for (JSONObject booking : store.where(BOOKINGS, "driver_id", driverId)) {
            if (tab.getStatuses().contains(statusOf(booking))) {
                result.add(toListItem(booking, Viewer.DRIVER));
            }
        }
        sortByStart(result, tab == DriverTab.HISTORY);
        return result;
    }

    @Override
    public Result<WeeklyHours> getWeeklyHours(long driverId) {
        RepoError forced = forced(BookingError.NOT_FOUND, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(USERS, driverId) == null) {
            return Result.error(BookingError.NOT_FOUND, "Driver " + driverId + " not found");
        }
        DateUtils.Range week = DateUtils.weekRange(store.now());
        double hours = 0;
        for (JSONObject booking : store.where(BOOKINGS, "driver_id", driverId)) {
            long start = getLong(booking, "start_time");
            if (WEEKLY_STATUSES.contains(statusOf(booking)) && week.contains(start)) {
                hours += (getLong(booking, "end_time") - start) / (double) HOUR;
            }
        }
        WeeklyHours weeklyHours = new WeeklyHours();
        weeklyHours.setWeekStart(week.getStart());
        weeklyHours.setWeekEnd(week.getEnd());
        weeklyHours.setBookedHours(hours);
        weeklyHours.setLimitHours(WEEKLY_LIMIT_HOURS);
        return Result.success(weeklyHours);
    }

    @Override
    public Result<BookingDetail> getBookingDetail(long viewerId, long bookingId) {
        RepoError forced = forced(BookingError.NOT_FOUND, BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        JSONObject vehicle = store.find(VEHICLES, getLong(booking, "vehicle_id"));
        long driverId = getLong(booking, "driver_id");
        long ownerId = getLong(vehicle, "owner_id");
        Viewer viewer = viewerOf(viewerId, driverId, ownerId);
        if (viewer == null) {
            return Result.error(BookingError.NOT_ALLOWED, "User " + viewerId + " cannot see booking " + bookingId);
        }

        BookingDetail detail = new BookingDetail();
        fill(detail, booking, vehicle, viewer);
        long start = getLong(booking, "start_time");
        detail.setPickupAddress(getString(vehicle, "address"));
        detail.setLatitude(getDoubleOrNull(vehicle, "latitude"));
        detail.setLongitude(getDoubleOrNull(vehicle, "longitude"));
        detail.setDriverId(driverId);
        detail.setOwnerId(ownerId);
        detail.setStartedAt(getLongOrNull(booking, "started_at"));
        detail.setCompletedAt(getLongOrNull(booking, "completed_at"));
        detail.setCreatedAt(getLong(booking, "created_at"));
        detail.setPayDeadline(start - 15 * MINUTE);
        detail.setStartWindowCloses(start + 60 * MINUTE);

        List<JSONObject> payments = store.where(PAYMENTS, "booking_id", bookingId);
        if (!payments.isEmpty()) {
            JSONObject payment = payments.get(0);
            detail.setPaidAt(getLongOrNull(payment, "paid_at"));
            detail.setCardLast4(getString(payment, "card_last4"));
            if (viewer != Viewer.DRIVER) {
                int fee = getInt(payment, "platform_fee_cents");
                detail.setFeeCents(fee);
                detail.setOwnerPayoutCents(getInt(payment, "amount_cents") - fee);
            }
        }
        for (JSONObject photo : store.where(BOOKING_PHOTOS, "booking_id", bookingId)) {
            if (PhotoType.PICKUP.name().equals(getString(photo, "photo_type"))) {
                detail.setPickupPhotoCount(detail.getPickupPhotoCount() + 1);
            } else {
                detail.setReturnPhotoCount(detail.getReturnPhotoCount() + 1);
            }
        }
        for (JSONObject review : store.where(REVIEWS, "booking_id", bookingId)) {
            long reviewerId = getLong(review, "reviewer_id");
            if (reviewerId == driverId) {
                detail.setReviewedByDriver(true);
            } else if (reviewerId == ownerId) {
                detail.setReviewedByOwner(true);
            }
        }
        detail.setOverlappingPendingIds(viewer == Viewer.OWNER
                ? getLongList(booking, "_overlapping_pending_ids")
                : new ArrayList<>());
        return Result.success(detail);
    }

    @Override
    public Result<Void> cancelBooking(long driverId, long bookingId) {
        RepoError forced = forced(BookingError.NOT_CANCELLABLE, BookingError.NOT_FOUND, BookingError.NOT_ALLOWED,
                BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        FakeBookingUpdates.close(store, booking, BookingStatus.CANCELLED, null);
        return Result.success(null);
    }

    @Override
    public Result<Void> canStart(long driverId, long bookingId) {
        RepoError forced = forced(BookingError.NOT_PAID, BookingError.OUTSIDE_START_WINDOW, BookingError.WRONG_STATUS,
                BookingError.NOT_FOUND, BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(BOOKINGS, bookingId) == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        return Result.success(null);
    }

    @Override
    public Result<Void> startTrip(long driverId, long bookingId, List<Uri> pickupPhotos) {
        RepoError forced = forced(BookingError.PHOTO_COUNT, BookingError.NOT_PAID, BookingError.OUTSIDE_START_WINDOW,
                BookingError.WRONG_STATUS, BookingError.FILE_ERROR, BookingError.NOT_FOUND, BookingError.NOT_ALLOWED,
                BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        insertPhotos(bookingId, PhotoType.PICKUP, pickupPhotos);
        set(booking, "status", BookingStatus.IN_PROGRESS.name());
        set(booking, "started_at", store.now());
        set(booking, "_driver_actions", new JSONArray(Collections.singletonList(DriverAction.END_TRIP.name())));
        return Result.success(null);
    }

    @Override
    public Result<Void> endTrip(long driverId, long bookingId, List<Uri> returnPhotos) {
        RepoError forced = forced(BookingError.PHOTO_COUNT, BookingError.WRONG_STATUS, BookingError.FILE_ERROR,
                BookingError.NOT_FOUND, BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        insertPhotos(bookingId, PhotoType.RETURN, returnPhotos);
        set(booking, "status", BookingStatus.COMPLETED.name());
        set(booking, "completed_at", store.now());
        set(booking, "_overdue", false);
        set(booking, "_driver_actions", new JSONArray(Collections.singletonList(DriverAction.REVIEW.name())));
        set(booking, "_owner_actions", new JSONArray(Collections.singletonList(OwnerAction.REVIEW.name())));
        return Result.success(null);
    }

    @Override
    public List<BookingListItem> getPendingByOwner(long ownerId) {
        List<BookingListItem> result = ownerBookings(ownerId, Collections.singletonList(BookingStatus.PENDING));
        sortByStart(result, false);
        return result;
    }

    @Override
    public List<BookingListItem> getOwnerBookings(long ownerId, OwnerTab tab) {
        List<BookingListItem> result = ownerBookings(ownerId, tab.getStatuses());
        sortByStart(result, tab == OwnerTab.HISTORY);
        return result;
    }

    @Override
    public Result<ConfirmResult> confirm(long ownerId, long bookingId) {
        RepoError forced = forced(BookingError.NOT_PENDING, BookingError.PAYMENT_DEADLINE_PASSED,
                BookingError.DRIVER_NOT_VERIFIED, BookingError.DOCUMENTS_EXPIRE, BookingError.DRIVER_INACTIVE,
                BookingError.VEHICLE_NOT_APPROVED, BookingError.VEHICLE_DELETED, BookingError.NOT_FOUND,
                BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        JSONObject vehicle = store.find(VEHICLES, getLong(booking, "vehicle_id"));
        List<Long> autoRejected = getLongList(booking, "_overlapping_pending_ids");
        set(booking, "status", BookingStatus.CONFIRMED.name());
        set(booking, "_driver_actions", new JSONArray(Arrays.asList(DriverAction.PAY.name(), DriverAction.CANCEL.name())));
        set(booking, "_owner_actions", new JSONArray());
        set(booking, "_overlapping_pending_ids", new JSONArray());
        FakeNotifications.add(store, getLong(booking, "driver_id"), "Booking confirmed",
                "Your " + FakeVehicles.title(vehicle) + " shift (" + shift(booking) + ") was confirmed. "
                        + "Pay before the start window opens.");

        for (long otherId : autoRejected) {
            JSONObject other = store.find(BOOKINGS, otherId);
            if (other != null) {
                FakeBookingUpdates.close(store, other, BookingStatus.REJECTED, StatusReasons.OVERLAP);
                notifyRejected(other, vehicle, StatusReasons.OVERLAP);
            }
        }
        ConfirmResult result = new ConfirmResult();
        result.setAutoRejectedIds(autoRejected);
        return Result.success(result);
    }

    @Override
    public Result<Void> reject(long ownerId, long bookingId, String reason) {
        RepoError forced = forced(BookingError.REASON_REQUIRED, BookingError.NOT_PENDING, BookingError.NOT_FOUND,
                BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject booking = store.find(BOOKINGS, bookingId);
        if (booking == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        String text = reason == null ? null : reason.trim();
        FakeBookingUpdates.close(store, booking, BookingStatus.REJECTED, text);
        notifyRejected(booking, store.find(VEHICLES, getLong(booking, "vehicle_id")), text);
        return Result.success(null);
    }

    @Override
    public List<BookingListItem> getAllBookings(@Nullable BookingStatus status) {
        List<BookingListItem> result = new ArrayList<>();
        for (JSONObject booking : store.rows(BOOKINGS)) {
            if (status == null || statusOf(booking) == status) {
                result.add(toListItem(booking, Viewer.ADMIN));
            }
        }
        sortByStart(result, true);
        return result;
    }

    @Override
    public Result<InspectionPhotos> getInspectionPhotos(long viewerId, long bookingId) {
        RepoError forced = forced(BookingError.NOT_FOUND, BookingError.NOT_ALLOWED, BookingError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (store.find(BOOKINGS, bookingId) == null) {
            return Result.error(BookingError.NOT_FOUND, "Booking " + bookingId + " not found");
        }
        List<String> pickup = new ArrayList<>();
        List<String> returned = new ArrayList<>();
        for (JSONObject photo : store.where(BOOKING_PHOTOS, "booking_id", bookingId)) {
            if (PhotoType.PICKUP.name().equals(getString(photo, "photo_type"))) {
                pickup.add(getString(photo, "file_path"));
            } else {
                returned.add(getString(photo, "file_path"));
            }
        }
        InspectionPhotos photos = new InspectionPhotos();
        photos.setBookingId(bookingId);
        photos.setPickupPaths(pickup);
        photos.setReturnPaths(returned);
        return Result.success(photos);
    }

    private List<BookingListItem> ownerBookings(long ownerId, List<BookingStatus> statuses) {
        List<BookingListItem> result = new ArrayList<>();
        for (JSONObject vehicle : store.where(VEHICLES, "owner_id", ownerId)) {
            for (JSONObject booking : store.where(BOOKINGS, "vehicle_id", getLong(vehicle, "id"))) {
                if (statuses.contains(statusOf(booking))) {
                    result.add(toListItem(booking, Viewer.OWNER));
                }
            }
        }
        return result;
    }

    private BookingListItem toListItem(JSONObject booking, Viewer viewer) {
        BookingListItem item = new BookingListItem();
        fill(item, booking, store.find(VEHICLES, getLong(booking, "vehicle_id")), viewer);
        return item;
    }

    private void fill(BookingListItem item, JSONObject booking, JSONObject vehicle, Viewer viewer) {
        long vehicleId = getLong(vehicle, "id");
        long driverId = getLong(booking, "driver_id");
        long ownerId = getLong(vehicle, "owner_id");
        long start = getLong(booking, "start_time");
        long end = getLong(booking, "end_time");
        item.setId(getLong(booking, "id"));
        item.setVehicleId(vehicleId);
        item.setVehicleTitle(FakeVehicles.title(vehicle));
        item.setThumbnailPath(FakeVehicles.thumbnail(store, vehicleId));
        item.setDriverName(FakeVehicles.shortName(store.find(USERS, driverId)));
        item.setOwnerName(FakeVehicles.shortName(store.find(USERS, ownerId)));
        if (viewer == Viewer.DRIVER) {
            item.setCounterpartRatingAvg(FakeRatings.of(store, ownerId).getAverage());
        } else if (viewer == Viewer.OWNER) {
            item.setCounterpartRatingAvg(FakeRatings.of(store, driverId).getAverage());
        }
        item.setStart(start);
        item.setEnd(end);
        item.setHours((int) ((end - start) / HOUR));
        item.setServiceType(ServiceType.valueOf(getString(booking, "service_type")));
        item.setTotalPriceCents(getInt(booking, "total_price_cents"));
        item.setStatus(statusOf(booking));
        item.setStatusReason(getString(booking, "status_reason"));
        item.setPaid(getBoolean(booking, "_paid"));
        item.setOverdue(getBoolean(booking, "_overdue"));
        List<DriverAction> driverActions = new ArrayList<>();
        if (viewer == Viewer.DRIVER) {
            for (String action : getStringList(booking, "_driver_actions")) {
                driverActions.add(DriverAction.valueOf(action));
            }
        }
        List<OwnerAction> ownerActions = new ArrayList<>();
        if (viewer == Viewer.OWNER) {
            for (String action : getStringList(booking, "_owner_actions")) {
                ownerActions.add(OwnerAction.valueOf(action));
            }
        }
        item.setDriverActions(driverActions);
        item.setOwnerActions(ownerActions);
    }

    private Viewer viewerOf(long viewerId, long driverId, long ownerId) {
        if (viewerId == driverId) {
            return Viewer.DRIVER;
        }
        if (viewerId == ownerId) {
            return Viewer.OWNER;
        }
        JSONObject user = store.find(USERS, viewerId);
        return user != null && Role.ADMIN.name().equals(getString(user, "role")) ? Viewer.ADMIN : null;
    }

    private void insertPhotos(long bookingId, PhotoType type, List<Uri> photos) {
        if (photos == null) {
            return;
        }
        for (Uri photo : photos) {
            JSONObject row = new JSONObject();
            set(row, "booking_id", bookingId);
            set(row, "photo_type", type.name());
            set(row, "file_path", photo == null ? null : photo.toString());
            set(row, "taken_at", store.now());
            store.insert(BOOKING_PHOTOS, row);
        }
    }

    private void notifyRejected(JSONObject booking, JSONObject vehicle, String reason) {
        FakeNotifications.add(store, getLong(booking, "driver_id"), "Booking request rejected",
                "Your request for the " + FakeVehicles.title(vehicle) + " was rejected: " + reason + ".");
    }

    private static String shift(JSONObject booking) {
        long hours = (getLong(booking, "end_time") - getLong(booking, "start_time")) / HOUR;
        String service = getString(booking, "service_type");
        return hours + " h, " + service.charAt(0) + service.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String fullName(JSONObject user) {
        return getString(user, "first_name") + " " + getString(user, "last_name");
    }

    private static BookingStatus statusOf(JSONObject booking) {
        return BookingStatus.valueOf(getString(booking, "status"));
    }

    private static void sortByStart(List<BookingListItem> items, boolean latestFirst) {
        items.sort((a, b) -> {
            int byStart = Long.compare(a.getStart(), b.getStart());
            int order = byStart != 0 ? byStart : Long.compare(a.getId(), b.getId());
            return latestFirst ? -order : order;
        });
    }

    private static RepoError forced(BookingError first, BookingError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
