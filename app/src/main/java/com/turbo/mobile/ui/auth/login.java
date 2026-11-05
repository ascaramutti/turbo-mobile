package com.turbo.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.databinding.ActivityLoginBinding;
import com.turbo.mobile.util.FormErrors;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.SplashActivity;

public class login extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        viewModel = new AuthViewModel(container.userRepository, container.sessionManager);

        binding.btnLogin.setOnClickListener(v -> logIn());
        binding.txtNoAccount.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), "Not implemented", Snackbar.LENGTH_SHORT).show());
    }
    private void logIn() {
        String email = binding.tfEmail.getText() != null ? binding.tfEmail.getText().toString().trim() : "";
        String password = binding.tfPassword.getText() != null ? binding.tfPassword.getText().toString() : "";

        binding.tfEmail.setError(null);
        binding.tfPassword.setError(null);

        // UI checks (no repository call)
        FormErrors errors = viewModel.validateLogin(email, password);
        if (errors.hasErrors()) {
            if (errors.has("email")) {
                binding.tfEmail.setError(errors.get("email"));
                binding.tfEmail.requestFocus();
            }
            if (errors.has("password")) {
                binding.tfPassword.setError(errors.get("password"));
                if (!errors.has("email")) {
                    binding.tfPassword.requestFocus();
                }
            }
            return;
        }

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        container.executor.execute(() -> {
            Result<SessionUser> result = viewModel.login(email, password);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    startActivity(new Intent(this, SplashActivity.class));
                    finish();
                } else {
                    RepoError code = ((Result.Error<SessionUser>) result).code;
                    if (code == UserError.ACCOUNT_DEACTIVATED) {
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.error_account_deactivated)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    } else if (code == UserError.INVALID_CREDENTIALS) {
                        Snackbar.make(binding.getRoot(), R.string.error_invalid_credentials, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(binding.getRoot(), R.string.error_unexpected, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }
}