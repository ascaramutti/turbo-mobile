package com.turbo.mobile.data.repository.room;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.db.entity.DriverProfileEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.RegistrationForm;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.util.ImageStorage;
import com.turbo.mobile.util.PasswordHasher;
import com.turbo.mobile.util.Validators;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomUserRepository implements UserRepository {

    private static final String TAG = "RoomUserRepository";
    private static final int ADULT_AGE = 18;
    private static final long RENEW_WINDOW = 30L * 24 * 60 * 60 * 1000;

    private final AppDatabase db;
    private final ImageStorage imageStorage;

    public RoomUserRepository(AppDatabase db, ImageStorage imageStorage) {
        this.db = db;
        this.imageStorage = imageStorage;
    }

    @Override
    public Result<SessionUser> authenticate(String email, String password) {
        try {
            UserEntity user = db.userDao().findByEmail(normalize(email));
            if (user == null || !PasswordHasher.verify(password, user.passwordHash, user.passwordSalt)) {
                return Result.error(UserError.INVALID_CREDENTIALS, "Invalid email or password");
            }
            if (!user.isActive) {
                return Result.error(UserError.ACCOUNT_DEACTIVATED, "Account deactivated");
            }
            return Result.success(toSessionUser(user));
        } catch (RuntimeException e) {
            return unexpected(e);
        }
    }

    @Override
    public Result<SessionUser> register(RegistrationForm form) {
        try {
            long now = System.currentTimeMillis();
            boolean driver = form.getRole() == Role.DRIVER;
            if (!isValid(form, driver)) {
                return Result.error(UserError.INVALID_INPUT, "Invalid registration form");
            }
            if (isUnder18(form.getDateOfBirth())) {
                return Result.error(UserError.UNDER_18, "Under 18");
            }
            if (driver && (form.getLicenseExpiry() <= now || form.getStudyPermitExpiry() <= now)) {
                return Result.error(UserError.EXPIRY_NOT_FUTURE, "Expiry date is not in the future");
            }
            String email = normalize(form.getEmail());
            if (db.userDao().findByEmail(email) != null) {
                return Result.error(UserError.EMAIL_TAKEN, "Email already registered");
            }
            String licenseNumber = driver ? form.getLicenseNumber().trim() : null;
            if (driver && db.driverProfileDao().findByLicenseNumber(licenseNumber) != null) {
                return Result.error(UserError.LICENSE_NUMBER_TAKEN, "License number already registered");
            }

            PasswordHasher.Hash hash = PasswordHasher.hash(form.getPassword());
            UserEntity user = new UserEntity();
            user.email = email;
            user.passwordHash = hash.hash;
            user.passwordSalt = hash.salt;
            user.firstName = form.getFirstName().trim();
            user.lastName = form.getLastName().trim();
            user.phone = form.getPhone().trim();
            user.dateOfBirth = form.getDateOfBirth();
            user.role = form.getRole();
            user.isActive = true;
            user.createdAt = now;
            db.runInTransaction(() -> {
                user.id = db.userDao().insert(user);
                if (driver) {
                    DriverProfileEntity profile = new DriverProfileEntity();
                    profile.userId = user.id;
                    profile.licenseNumber = licenseNumber;
                    profile.licenseClass = form.getLicenseClass();
                    profile.licenseExpiry = form.getLicenseExpiry();
                    profile.studyPermitNumber = form.getStudyPermitNumber().trim();
                    profile.studyPermitExpiry = form.getStudyPermitExpiry();
                    db.driverProfileDao().insert(profile);
                }
            });
            return Result.success(toSessionUser(user));
        } catch (RuntimeException e) {
            return unexpected(e);
        }
    }

    @Override
    public Result<UserProfile> getProfile(long userId) {
        try {
            UserEntity user = db.userDao().findById(userId);
            if (user == null) {
                return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
            }
            UserProfile profile = new UserProfile();
            profile.setId(user.id);
            profile.setEmail(user.email);
            profile.setFirstName(user.firstName);
            profile.setLastName(user.lastName);
            profile.setPhone(user.phone);
            profile.setProfilePhotoPath(user.profilePhotoPath);
            profile.setRole(user.role);
            if (user.role == Role.DRIVER) {
                DriverProfileEntity driverProfile = db.driverProfileDao().findByUserId(user.id);
                if (driverProfile != null) {
                    profile.setLicenseClass(driverProfile.licenseClass);
                }
                profile.setDriverState(calculateDriverState(user));
            }
            RatingSummary rating = db.reviewDao().getRatingSummary(user.id);
            profile.setRatingAvg(rating.getAverage());
            profile.setRatingCount(rating.getCount());
            return Result.success(profile);
        } catch (RuntimeException e) {
            return unexpected(e);
        }
    }

    @Override
    public Result<Void> updateProfile(long userId, String firstName, String lastName, String phone, @Nullable Uri newPhoto) {
        try {
            if (Validators.isBlank(firstName) || Validators.isBlank(lastName) || !Validators.isPhone(phone)) {
                return Result.error(UserError.INVALID_INPUT, "Invalid profile data");
            }
            UserEntity user = db.userDao().findById(userId);
            if (user == null) {
                return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
            }
            if (newPhoto != null) {
                String oldPhoto = user.profilePhotoPath;
                try {
                    user.profilePhotoPath = imageStorage.save(newPhoto, ImageStorage.Folder.PROFILES);
                } catch (IOException e) {
                    Log.e(TAG, "Cannot save the profile photo", e);
                    return Result.error(UserError.FILE_ERROR, "Cannot save the photo");
                }
                imageStorage.delete(oldPhoto);
            }
            user.firstName = firstName.trim();
            user.lastName = lastName.trim();
            user.phone = phone.trim();
            db.userDao().update(user);
            return Result.success(null);
        } catch (RuntimeException e) {
            return unexpected(e);
        }
    }

    @Override
    public List<UserListItem> getUsers(@Nullable String query, @Nullable Role role) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<UserListItem> result = new ArrayList<>();
        for (UserEntity user : db.userDao().search(text, role == null ? null : role.name())) {
            UserListItem item = new UserListItem();
            item.setId(user.id);
            item.setFullName(fullName(user));
            item.setEmail(user.email);
            item.setRole(user.role);
            item.setActive(user.isActive);
            result.add(item);
        }
        return result;
    }

    @Override
    public Result<DeactivationResult> setActive(long adminId, long userId, boolean active) {
        try {
            UserEntity admin = db.userDao().findById(adminId);
            if (admin == null || admin.role != Role.ADMIN || !admin.isActive) {
                return Result.error(UserError.NOT_ALLOWED, "Only an active Admin can do this");
            }
            UserEntity user = db.userDao().findById(userId);
            if (user == null) {
                return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
            }
            if (user.role == Role.ADMIN) {
                return Result.error(UserError.CANNOT_CHANGE_ADMIN, "Admins cannot be changed");
            }
            DeactivationResult result = new DeactivationResult();
            db.runInTransaction(() -> {
                user.isActive = active;
                db.userDao().update(user);
                if (!active && user.role == Role.DRIVER) {
                    closeDriverBookings(user, result);
                } else if (!active && user.role == Role.CAR_OWNER) {
                    closeOwnerBookings(user, result);
                }
            });
            return Result.success(result);
        } catch (RuntimeException e) {
            return unexpected(e);
        }
    }

    private DriverState calculateDriverState(UserEntity driver) {
        if (!driver.isActive) {
            return DriverState.DEACTIVATED;
        }
        int approved = 0;
        for (DriverDocumentEntity document : db.driverDocumentDao().getAll(driver.id)) {
            if (document.status == DocumentStatus.REJECTED) {
                return DriverState.DOCUMENT_REJECTED;
            }
            if (document.status == DocumentStatus.APPROVED) {
                approved++;
            }
        }
        if (approved < 2) {
            return DriverState.PENDING_VERIFICATION;
        }
        DriverProfileEntity profile = db.driverProfileDao().findByUserId(driver.id);
        long now = System.currentTimeMillis();
        if (profile != null && Math.min(profile.licenseExpiry, profile.studyPermitExpiry) <= now + RENEW_WINDOW) {
            return DriverState.VERIFIED_RENEW;
        }
        return DriverState.VERIFIED;
    }

    private void closeDriverBookings(UserEntity driver, DeactivationResult result) {
        for (BookingEntity booking : db.bookingDao().getOpenByDriver(driver.id)) {
            close(booking, BookingStatus.CANCELLED);
            result.setCancelledCount(result.getCancelledCount() + 1);
            VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
            notify(vehicle.ownerId, "Booking cancelled", fullName(driver) + "'s booking for your "
                    + title(vehicle) + " was cancelled: " + StatusReasons.ACCOUNT_DEACTIVATED + ".");
        }
    }

    private void closeOwnerBookings(UserEntity owner, DeactivationResult result) {
        for (BookingEntity booking : db.bookingDao().getOpenByOwner(owner.id)) {
            VehicleEntity vehicle = db.vehicleDao().findById(booking.vehicleId);
            if (booking.status == BookingStatus.PENDING) {
                close(booking, BookingStatus.REJECTED);
                result.setRejectedCount(result.getRejectedCount() + 1);
                notify(booking.driverId, "Booking request rejected", "Your request for the " + title(vehicle)
                        + " was rejected: " + StatusReasons.ACCOUNT_DEACTIVATED + ".");
            } else {
                close(booking, BookingStatus.CANCELLED);
                result.setCancelledCount(result.getCancelledCount() + 1);
                notify(booking.driverId, "Booking cancelled", "Your booking for the " + title(vehicle)
                        + " was cancelled: " + StatusReasons.ACCOUNT_DEACTIVATED + ".");
            }
        }
    }

    private void close(BookingEntity booking, BookingStatus status) {
        booking.status = status;
        booking.statusReason = StatusReasons.ACCOUNT_DEACTIVATED;
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

    private boolean isValid(RegistrationForm form, boolean driver) {
        if (form.getRole() == null || form.getRole() == Role.ADMIN || !form.isAcceptedTerms()
                || Validators.isBlank(form.getFirstName()) || Validators.isBlank(form.getLastName())
                || !Validators.isEmail(form.getEmail()) || !Validators.isPhone(form.getPhone())
                || !Validators.isPassword(form.getPassword())) {
            return false;
        }
        return !driver || (!Validators.isBlank(form.getLicenseNumber()) && form.getLicenseClass() != null
                && !Validators.isBlank(form.getStudyPermitNumber()));
    }

    private static boolean isUnder18(long dateOfBirth) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate birth = Instant.ofEpochMilli(dateOfBirth).atZone(zone).toLocalDate();
        return birth.plusYears(ADULT_AGE).isAfter(LocalDate.now(zone));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String fullName(UserEntity user) {
        return user.firstName + " " + user.lastName;
    }

    private static String title(VehicleEntity vehicle) {
        return vehicle.make + " " + vehicle.model + " " + vehicle.year;
    }

    private static SessionUser toSessionUser(UserEntity user) {
        SessionUser sessionUser = new SessionUser();
        sessionUser.setId(user.id);
        sessionUser.setRole(user.role);
        sessionUser.setFirstName(user.firstName);
        sessionUser.setLastName(user.lastName);
        return sessionUser;
    }

    private static <T> Result<T> unexpected(RuntimeException e) {
        Log.e(TAG, "Unexpected database error", e);
        return Result.error(UserError.UNEXPECTED, e.getMessage());
    }
}
