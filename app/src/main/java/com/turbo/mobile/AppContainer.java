package com.turbo.mobile;

import android.content.Context;

import com.turbo.mobile.data.repository.BookingRepository;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.data.repository.NotificationRepository;
import com.turbo.mobile.data.repository.PaymentRepository;
import com.turbo.mobile.data.repository.ReviewRepository;
import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.data.repository.VehicleRepository;
import com.turbo.mobile.data.repository.fake.FakeBookingRepository;
import com.turbo.mobile.data.repository.fake.FakeDataStore;
import com.turbo.mobile.data.repository.fake.FakeDocumentRepository;
import com.turbo.mobile.data.repository.fake.FakeNotificationRepository;
import com.turbo.mobile.data.repository.fake.FakePaymentRepository;
import com.turbo.mobile.data.repository.fake.FakeReviewRepository;
import com.turbo.mobile.data.repository.fake.FakeUserRepository;
import com.turbo.mobile.data.repository.fake.FakeVehicleRepository;
import com.turbo.mobile.util.ImageStorage;
import com.turbo.mobile.util.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppContainer {

    // Single thread: Room forbids main-thread queries and FakeDataStore isn't thread-safe.
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    public final SessionManager sessionManager;
    public final ImageStorage imageStorage;
    public final UserRepository userRepository;
    public final NotificationRepository notificationRepository;
    public final DocumentRepository documentRepository;
    public final VehicleRepository vehicleRepository;
    public final BookingRepository bookingRepository;
    public final PaymentRepository paymentRepository;
    public final ReviewRepository reviewRepository;

    public AppContainer(Context context) {
        sessionManager = new SessionManager(context);
        imageStorage = new ImageStorage(context);
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
        reviewRepository = new FakeReviewRepository(store);
    }
}
