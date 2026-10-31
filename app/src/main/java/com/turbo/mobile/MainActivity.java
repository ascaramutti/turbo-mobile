package com.turbo.mobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.ui.admin.PendingDocumentsActivity;
import com.turbo.mobile.ui.driver.DriverActivity;
import com.turbo.mobile.ui.owner.OwnerActivity;
import com.turbo.mobile.util.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = ((TurboApp) getApplication()).getContainer().sessionManager;
        // TODO(login): temporary until the login screen lands — signs in as ana@mail.com (driver).
        // Owner screens: session.saveSession(2, Role.CAR_OWNER) (maria@mail.com).
        // Admin screens: session.saveSession(1, Role.ADMIN) (admin@turbo.ca).
        session.saveSession(6, Role.DRIVER);

        // Each role has its own home screen.
        Intent home;
        if (session.getRole() == Role.CAR_OWNER) {
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
