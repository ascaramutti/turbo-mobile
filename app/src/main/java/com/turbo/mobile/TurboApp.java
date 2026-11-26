package com.turbo.mobile;

import android.app.Application;

import com.turbo.mobile.util.NotificationHelper;

public class TurboApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        NotificationHelper.createChannel(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}
