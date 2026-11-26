package com.turbo.mobile.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.db.AppDatabase;
import com.turbo.mobile.data.db.entity.NotificationEntity;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.util.NotificationHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShiftReminderService extends Service {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_DRIVER_ID = "driver_id";

    private static final String TAG = "ShiftReminderService";
    private static final long DEMO_DELAY = 30 * 1000;

    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate method is called");
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand method is called");
        long bookingId = intent.getLongExtra(EXTRA_BOOKING_ID, 0);
        long driverId = intent.getLongExtra(EXTRA_DRIVER_ID, 0);

        executor.execute(() -> {
            try {
                Thread.sleep(DEMO_DELAY);
                sendReminder(driverId,  bookingId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopSelf(startId);
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy method is called");
        if (executor != null) {
            executor.shutdownNow();
        }
        super.onDestroy();
    }

    private void sendReminder(long driverId, long bookingId) {
        Result<BookingDetail> result = ((TurboApp) getApplication()).getContainer()
                .bookingRepository.getBookingDetail(driverId, bookingId);

        if (!(result instanceof Result.Success)) {
            return;
        }

        BookingDetail booking = ((Result.Success<BookingDetail>) result).data;
        if (booking.getStatus() != BookingStatus.CONFIRMED || !booking.isPaid()) {
            Log.d(TAG, "Booking " + bookingId + " is no longer confirmed and paid");
            return;
        }
        String title = getString(R.string.notification_reminder_title);
        String message = getString(R.string.notification_reminder_message,
                booking.getVehicleTitle(),
                DisplayFormat.time(booking.getStart()),
                booking.getPickupAddress());

        NotificationEntity notification = new NotificationEntity();
        notification.userId = driverId;
        notification.title = title;
        notification.message = message;
        notification.createdAt = System.currentTimeMillis();
        AppDatabase.getInstance(this).notificationDao().insert(notification);

        NotificationHelper.show(this, (int) bookingId + 1000, title, message);
    }
}