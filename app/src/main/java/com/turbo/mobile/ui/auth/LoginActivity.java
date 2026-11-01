package com.turbo.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.MainActivity;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.ui.TurboUi;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TurboUi.fitSystemBars(this);

        findViewById(R.id.btnLogin).setOnClickListener(v -> logIn());
        findViewById(R.id.txtNoAccount).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    // Checks the email and password on the background thread. If they are right, it saves who is
    // signed in and MainActivity opens the home screen of their role. If not, it shows the error
    // ("Invalid email or password" or "Account deactivated").
    private void logIn() {
        EditText emailField = findViewById(R.id.tfEmail);
        EditText passwordField = findViewById(R.id.tfPassword);
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        container.executor.execute(() -> {
            Result<SessionUser> result = container.userRepository.authenticate(email, password);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    SessionUser user = ((Result.Success<SessionUser>) result).data;
                    container.sessionManager.saveSession(user.getId(), user.getRole());
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    TurboUi.showError(this, result);
                }
            });
        });
    }
}
