package com.turbo.mobile;

import android.content.Context;

import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.data.repository.NotificationRepository;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.data.repository.fake.FakeBookingRepository;
import com.turbo.mobile.data.repository.fake.FakeDataStore;
import com.turbo.mobile.data.repository.fake.FakeDocumentRepository;
import com.turbo.mobile.data.repository.fake.FakeNotificationRepository;
import com.turbo.mobile.data.repository.fake.FakePaymentRepository;
import com.turbo.mobile.data.repository.fake.FakeUserRepository;
import com.turbo.mobile.data.repository.fake.FakeVehicleRepository;
import com.turbo.mobile.util.SessionManager;

public class AppContainer {

    public final SessionManager sessionManager;
    public final UserRepository userRepository;
    public final NotificationRepository notificationRepository;
    public final DocumentRepository documentRepository;
    public final VehicleRepository vehicleRepository;
    public final BookingRepository bookingRepository;
    public final PaymentRepository paymentRepository;

    public AppContainer(Context context) {
        sessionManager = new SessionManager(context);
        if (!BuildConfig.USE_FAKE) {
            throw new IllegalStateException("Room repositories are not implemented yet");
        }
        FakeDataStore store = new FakeDataStore(context);
        userRepository = new FakeUserRepository(store);
        notificationRepository = new FakeNotificationRepository(store);
        documentRepository = new FakeDocumentRepository(store);
        vehicleRepository = new FakeVehicleRepository(store);
        bookingRepository = new FakeBookingRepository(store);
        paymentRepository = new FakePaymentRepository(store);
    }
}
