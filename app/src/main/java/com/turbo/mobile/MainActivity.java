package com.turbo.mobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.ui.admin.PendingDocumentsActivity;
import com.turbo.mobile.ui.auth.LoginActivity;
import com.turbo.mobile.ui.driver.DriverActivity;
import com.turbo.mobile.ui.owner.OwnerActivity;
import com.turbo.mobile.util.SessionManager;

/** First screen of the app. It shows nothing: it only decides which screen to open. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = ((TurboApp) getApplication()).getContainer().sessionManager;

        // Nobody signed in: login screen. After logging in, LoginActivity comes back here.
        // Each role has its own home screen.
        Intent home;
        if (!session.isLoggedIn()) {
            home = new Intent(this, LoginActivity.class);
        } else if (session.getRole() == Role.CAR_OWNER) {
            home = new Intent(this, OwnerActivity.class);
        } else if (session.getRole() == Role.DRIVER) {
            home = new Intent(this, DriverActivity.class);
        } else {
            home = new Intent(this, PendingDocumentsActivity.class);
        }
        startActivity(home);
        finish();
    }
}
