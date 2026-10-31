package com.turbo.mobile.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.turbo.mobile.data.db.dao.BookingDao;
import com.turbo.mobile.data.db.dao.BookingPhotoDao;
import com.turbo.mobile.data.db.dao.DriverDocumentDao;
import com.turbo.mobile.data.db.dao.DriverProfileDao;
import com.turbo.mobile.data.db.dao.NotificationDao;
import com.turbo.mobile.data.db.dao.PaymentDao;
import com.turbo.mobile.data.db.dao.ReviewDao;
import com.turbo.mobile.data.db.dao.UserDao;
import com.turbo.mobile.data.db.dao.VehicleDao;
import com.turbo.mobile.data.db.dao.VehicleDocumentDao;
import com.turbo.mobile.data.db.dao.VehiclePhotoDao;
import com.turbo.mobile.data.db.dao.ZoneDao;
import com.turbo.mobile.data.db.entity.BookingEntity;
import com.turbo.mobile.data.db.entity.BookingPhotoEntity;
import com.turbo.mobile.data.db.entity.DriverDocumentEntity;
import com.turbo.mobile.data.db.entity.DriverProfileEntity;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.db.entity.PaymentEntity;
import com.turbo.mobile.data.db.entity.ReviewEntity;
import com.turbo.mobile.data.db.entity.UserEntity;
import com.turbo.mobile.data.db.entity.VehicleDocumentEntity;
import com.turbo.mobile.data.db.entity.VehicleEntity;
import com.turbo.mobile.data.db.entity.VehiclePhotoEntity;
import com.turbo.mobile.data.db.entity.ZoneEntity;

@Database(entities = {
        ZoneEntity.class, UserEntity.class, DriverProfileEntity.class, DriverDocumentEntity.class,
        VehicleEntity.class, VehicleDocumentEntity.class, VehiclePhotoEntity.class, BookingEntity.class,
        BookingPhotoEntity.class, PaymentEntity.class, ReviewEntity.class, NotificationEntity.class},
        version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NAME = "turbo.db";

    private static volatile AppDatabase instance;

    public abstract ZoneDao zoneDao();

    public abstract UserDao userDao();

    public abstract DriverProfileDao driverProfileDao();

    public abstract DriverDocumentDao driverDocumentDao();

    public abstract VehicleDao vehicleDao();

    public abstract VehicleDocumentDao vehicleDocumentDao();

    public abstract VehiclePhotoDao vehiclePhotoDao();

    public abstract BookingDao bookingDao();

    public abstract BookingPhotoDao bookingPhotoDao();

    public abstract PaymentDao paymentDao();

    public abstract ReviewDao reviewDao();

    public abstract NotificationDao notificationDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, NAME)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
