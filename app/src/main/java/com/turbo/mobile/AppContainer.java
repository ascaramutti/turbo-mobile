package com.turbo.mobile;

import android.content.Context;

import com.turbo.mobile.data.repository.UserRepository;
import com.turbo.mobile.data.repository.fake.FakeDataStore;
import com.turbo.mobile.data.repository.fake.FakeUserRepository;
import com.turbo.mobile.util.SessionManager;

public class AppContainer {

    public final SessionManager sessionManager;
    public final UserRepository userRepository;

    public AppContainer(Context context) {
        sessionManager = new SessionManager(context);
        if (!BuildConfig.USE_FAKE) {
            throw new IllegalStateException("Room repositories are not implemented yet");
        }
        FakeDataStore store = new FakeDataStore(context);
        userRepository = new FakeUserRepository(store);
    }
}
