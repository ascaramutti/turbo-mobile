package com.turbo.mobile.data.repository.fake;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DeactivationResult;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DriverState;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.RatingSummary;
import com.turbo.mobile.data.model.RegistrationForm;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.data.model.UserListItem;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.model.VehicleBadge;
import com.turbo.mobile.data.repository.UserRepository;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static com.turbo.mobile.data.repository.fake.FakeDataStore.BOOKINGS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.DRIVER_DOCUMENTS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.DRIVER_PROFILES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.USERS;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.VEHICLES;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getBoolean;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getLong;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.getString;
import static com.turbo.mobile.data.repository.fake.FakeDataStore.set;

public class FakeUserRepository implements UserRepository {

    private final FakeDataStore store;

    public FakeUserRepository(FakeDataStore store) {
        this.store = store;
    }

    @Override
    public Result<SessionUser> authenticate(String email, String password) {
        RepoError forced = forced(UserError.INVALID_CREDENTIALS, UserError.ACCOUNT_DEACTIVATED, UserError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject user = findByEmail(email);
        if (user == null || !password.equals(getString(user, "_password"))) {
            return Result.error(UserError.INVALID_CREDENTIALS, "Invalid email or password");
        }
        if (!getBoolean(user, "is_active")) {
            return Result.error(UserError.ACCOUNT_DEACTIVATED, "Account deactivated");
        }
        return Result.success(toSessionUser(user));
    }

    @Override
    public Result<SessionUser> register(RegistrationForm form) {
        RepoError forced = forced(UserError.EMAIL_TAKEN, UserError.LICENSE_NUMBER_TAKEN, UserError.UNDER_18,
                UserError.EXPIRY_NOT_FUTURE, UserError.INVALID_INPUT, UserError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        if (findByEmail(form.getEmail()) != null) {
            return Result.error(UserError.EMAIL_TAKEN, "Email already registered");
        }
        boolean driver = form.getRole() == Role.DRIVER;
        if (driver && findProfileByLicense(form.getLicenseNumber()) != null) {
            return Result.error(UserError.LICENSE_NUMBER_TAKEN, "License number already registered");
        }

        JSONObject user = new JSONObject();
        set(user, "email", form.getEmail().trim().toLowerCase(Locale.ROOT));
        set(user, "first_name", form.getFirstName());
        set(user, "last_name", form.getLastName());
        set(user, "phone", form.getPhone());
        set(user, "date_of_birth", form.getDateOfBirth());
        set(user, "role", form.getRole().name());
        set(user, "profile_photo_path", null);
        set(user, "is_active", 1);
        set(user, "created_at", store.now());
        set(user, "_password", form.getPassword());
        if (driver) {
            set(user, "_verified", false);
            set(user, "_driver_state", DriverState.PENDING_VERIFICATION.name());
        }
        long userId = store.insert(USERS, user);

        if (driver) {
            JSONObject profile = new JSONObject();
            set(profile, "user_id", userId);
            set(profile, "license_number", form.getLicenseNumber());
            set(profile, "license_class", form.getLicenseClass().name());
            set(profile, "license_expiry", form.getLicenseExpiry());
            set(profile, "study_permit_number", form.getStudyPermitNumber());
            set(profile, "study_permit_expiry", form.getStudyPermitExpiry());
            store.insert(DRIVER_PROFILES, profile);
        }
        return Result.success(toSessionUser(user));
    }

    @Override
    public Result<UserProfile> getProfile(long userId) {
        RepoError forced = forced(UserError.NOT_FOUND, UserError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject user = store.find(USERS, userId);
        if (user == null) {
            return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
        }
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setEmail(getString(user, "email"));
        profile.setFirstName(getString(user, "first_name"));
        profile.setLastName(getString(user, "last_name"));
        profile.setPhone(getString(user, "phone"));
        profile.setProfilePhotoPath(getString(user, "profile_photo_path"));
        profile.setRole(Role.valueOf(getString(user, "role")));
        if (profile.getRole() == Role.DRIVER) {
            JSONObject driverProfile = findProfileByUser(userId);
            if (driverProfile != null) {
                profile.setLicenseClass(LicenseClass.valueOf(getString(driverProfile, "license_class")));
            }
            profile.setDriverState(DriverState.valueOf(getString(user, "_driver_state")));
        }
        RatingSummary rating = FakeRatings.of(store, userId);
        profile.setRatingAvg(rating.getAverage());
        profile.setRatingCount(rating.getCount());
        return Result.success(profile);
    }

    @Override
    public Result<Void> updateProfile(long userId, String firstName, String lastName, String phone, @Nullable Uri newPhoto) {
        RepoError forced = forced(UserError.INVALID_INPUT, UserError.FILE_ERROR, UserError.NOT_FOUND, UserError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject user = store.find(USERS, userId);
        if (user == null) {
            return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
        }
        set(user, "first_name", firstName);
        set(user, "last_name", lastName);
        set(user, "phone", phone);
        if (newPhoto != null) {
            set(user, "profile_photo_path", newPhoto.toString());
        }
        return Result.success(null);
    }

    @Override
    public List<UserListItem> getUsers(@Nullable String query, @Nullable Role role) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<UserListItem> result = new ArrayList<>();
        for (JSONObject user : store.rows(USERS)) {
            Role userRole = Role.valueOf(getString(user, "role"));
            if (userRole == Role.ADMIN || (role != null && userRole != role)) {
                continue;
            }
            String fullName = getString(user, "first_name") + " " + getString(user, "last_name");
            String email = getString(user, "email");
            if (!fullName.toLowerCase(Locale.ROOT).contains(text) && !email.contains(text)) {
                continue;
            }
            UserListItem item = new UserListItem();
            item.setId(getLong(user, "id"));
            item.setFullName(fullName);
            item.setEmail(email);
            item.setRole(userRole);
            item.setActive(getBoolean(user, "is_active"));
            result.add(item);
        }
        result.sort((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()));
        return result;
    }

    @Override
    public Result<DeactivationResult> setActive(long adminId, long userId, boolean active) {
        RepoError forced = forced(UserError.CANNOT_CHANGE_ADMIN, UserError.NOT_FOUND, UserError.NOT_ALLOWED,
                UserError.UNEXPECTED);
        if (forced != null) {
            return Result.error(forced, "Forced error");
        }
        JSONObject user = store.find(USERS, userId);
        if (user == null) {
            return Result.error(UserError.NOT_FOUND, "User " + userId + " not found");
        }
        Role role = Role.valueOf(getString(user, "role"));
        set(user, "is_active", active ? 1 : 0);

        DeactivationResult result = new DeactivationResult();
        if (active) {
            if (role == Role.DRIVER) {
                set(user, "_driver_state", stateFromDocuments(userId).name());
            }
        } else if (role == Role.DRIVER) {
            set(user, "_driver_state", DriverState.DEACTIVATED.name());
            for (JSONObject booking : store.where(BOOKINGS, "driver_id", userId)) {
                BookingStatus status = BookingStatus.valueOf(getString(booking, "status"));
                if (status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED
                        || status == BookingStatus.IN_PROGRESS) {
                    FakeBookingUpdates.close(store, booking, BookingStatus.CANCELLED, StatusReasons.ACCOUNT_DEACTIVATED);
                    result.setCancelledCount(result.getCancelledCount() + 1);
                }
            }
        } else if (role == Role.CAR_OWNER) {
            for (JSONObject vehicle : store.where(VEHICLES, "owner_id", userId)) {
                set(vehicle, "_published", false);
                if (VehicleBadge.PUBLISHED.name().equals(getString(vehicle, "_badge"))) {
                    set(vehicle, "_badge", VehicleBadge.NOT_PUBLISHED.name());
                }
                for (JSONObject booking : store.where(BOOKINGS, "vehicle_id", getLong(vehicle, "id"))) {
                    BookingStatus status = BookingStatus.valueOf(getString(booking, "status"));
                    if (status == BookingStatus.PENDING) {
                        FakeBookingUpdates.close(store, booking, BookingStatus.REJECTED, StatusReasons.ACCOUNT_DEACTIVATED);
                        result.setRejectedCount(result.getRejectedCount() + 1);
                    } else if (status == BookingStatus.CONFIRMED || status == BookingStatus.IN_PROGRESS) {
                        FakeBookingUpdates.close(store, booking, BookingStatus.CANCELLED, StatusReasons.ACCOUNT_DEACTIVATED);
                        result.setCancelledCount(result.getCancelledCount() + 1);
                    }
                }
            }
        }
        return Result.success(result);
    }

    private JSONObject findByEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        for (JSONObject user : store.rows(USERS)) {
            if (normalized.equals(getString(user, "email"))) {
                return user;
            }
        }
        return null;
    }

    private JSONObject findProfileByUser(long userId) {
        List<JSONObject> profiles = store.where(DRIVER_PROFILES, "user_id", userId);
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private JSONObject findProfileByLicense(String licenseNumber) {
        for (JSONObject profile : store.rows(DRIVER_PROFILES)) {
            if (getString(profile, "license_number").equalsIgnoreCase(licenseNumber == null ? "" : licenseNumber.trim())) {
                return profile;
            }
        }
        return null;
    }

    private DriverState stateFromDocuments(long driverId) {
        List<JSONObject> documents = store.where(DRIVER_DOCUMENTS, "driver_id", driverId);
        int approved = 0;
        for (JSONObject document : documents) {
            String status = getString(document, "status");
            if (DocumentStatus.REJECTED.name().equals(status)) {
                return DriverState.DOCUMENT_REJECTED;
            }
            if (DocumentStatus.APPROVED.name().equals(status)) {
                approved++;
            }
        }
        return approved == 2 ? DriverState.VERIFIED : DriverState.PENDING_VERIFICATION;
    }

    private SessionUser toSessionUser(JSONObject user) {
        SessionUser sessionUser = new SessionUser();
        sessionUser.setId(getLong(user, "id"));
        sessionUser.setRole(Role.valueOf(getString(user, "role")));
        sessionUser.setFirstName(getString(user, "first_name"));
        sessionUser.setLastName(getString(user, "last_name"));
        return sessionUser;
    }

    private static RepoError forced(UserError first, UserError... rest) {
        return FakeConfig.take(EnumSet.of(first, rest));
    }
}
