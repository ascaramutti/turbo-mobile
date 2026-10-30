package com.turbo.mobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.ui.driver.DriverActivity;
import com.turbo.mobile.ui.owner.OwnerActivity;
import com.turbo.mobile.util.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = ((TurboApp) getApplication()).getContainer().sessionManager;
        // TODO(login): temporary until the login screen lands — signs in as ana@mail.com (driver).
        // To see the owner screens use session.saveSession(2, Role.CAR_OWNER) (maria@mail.com).
        session.saveSession(6, Role.DRIVER);
        if (session.getRole() == Role.CAR_OWNER) {
            startActivity(new Intent(this, OwnerActivity.class));
            finish();
            return;
        }
        if (session.getRole() == Role.DRIVER) {
            startActivity(new Intent(this, DriverActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}