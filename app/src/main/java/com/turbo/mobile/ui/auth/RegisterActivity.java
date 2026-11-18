package com.turbo.mobile.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.LicenseClass;
import com.turbo.mobile.data.model.RegistrationForm;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.Role;
import com.turbo.mobile.data.model.SessionUser;
import com.turbo.mobile.data.model.UserError;
import com.turbo.mobile.databinding.ActivityRegisterBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.SplashActivity;
import com.turbo.mobile.util.Validators;

import java.util.Calendar;

/**
 * Activity allowing Drivers and Car Owners to register a new account (W-02).
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    private Role selectedRole = Role.DRIVER;
    private long dobMillis = 0;
    private long licenseExpiryMillis = 0;
    private long permitExpiryMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());
        binding.topBar.avatar.setVisibility(View.GONE);

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        viewModel = new AuthViewModel(container.userRepository, container.sessionManager);

        setUpRoleToggle();
        setUpLicenseClassSpinner();
        setUpDatePickers();

        binding.btnRegister.setOnClickListener(v -> register());
    }

    private void setUpRoleToggle() {
        updateRoleSelection(Role.DRIVER);
        binding.tabDriver.setOnClickListener(v -> updateRoleSelection(Role.DRIVER));
        binding.tabOwner.setOnClickListener(v -> updateRoleSelection(Role.CAR_OWNER));
    }

    private void updateRoleSelection(Role role) {
        selectedRole = role;
        binding.tabDriver.setSelected(role == Role.DRIVER);
        binding.tabOwner.setSelected(role == Role.CAR_OWNER);
        binding.driverSection.setVisibility(role == Role.DRIVER ? View.VISIBLE : View.GONE);
    }

    private void setUpLicenseClassSpinner() {
        String[] classes = {"Class 4", "Class 5"};
        ScreenSetup.fillSpinner(binding.licenseClass, classes);
    }

    private void setUpDatePickers() {
        binding.dateOfBirth.setOnClickListener(v -> showDatePicker(binding.dateOfBirth, true, false));
        binding.licenseExpiry.setOnClickListener(v -> showDatePicker(binding.licenseExpiry, false, true));
        binding.studyPermitExpiry.setOnClickListener(v -> showDatePicker(binding.studyPermitExpiry, false, false));
    }

    private void showDatePicker(EditText target, boolean isDob, boolean isLicense) {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.clear();
                    picked.set(year, month, dayOfMonth);
                    long millis = picked.getTimeInMillis();
                    if (isDob) {
                        dobMillis = millis;
                    } else if (isLicense) {
                        licenseExpiryMillis = millis;
                    } else {
                        permitExpiryMillis = millis;
                    }
                    target.setText(DisplayFormat.fullDate(millis));
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void register() {
        binding.errorBanner.setVisibility(View.GONE);

        String firstName = textOf(binding.firstName);
        String lastName = textOf(binding.lastName);
        String email = textOf(binding.email);
        String phone = textOf(binding.phone);
        String password = binding.password.getText() != null ? binding.password.getText().toString() : "";
        String confirm = binding.confirmPassword.getText() != null ? binding.confirmPassword.getText().toString() : "";

        if (Validators.isBlank(firstName) || Validators.isBlank(lastName) || dobMillis == 0) {
            showError(R.string.register_error_required);
            return;
        }
        if (!Validators.isEmail(email)) {
            showError(R.string.error_invalid_email_format);
            return;
        }
        if (!Validators.isPhone(phone)) {
            showError(R.string.register_error_phone);
            return;
        }
        if (!Validators.isPassword(password)) {
            showError(R.string.register_error_password_short);
            return;
        }
        if (!password.equals(confirm)) {
            showError(R.string.register_error_password_mismatch);
            return;
        }

        RegistrationForm form = new RegistrationForm();
        form.setRole(selectedRole);
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);
        form.setPhone(phone);
        form.setDateOfBirth(dobMillis);
        form.setPassword(password);
        form.setAcceptedTerms(binding.termsCheckbox.isChecked());

        if (selectedRole == Role.DRIVER) {
            String licenseNumber = textOf(binding.licenseNumber);
            String studyPermitNumber = textOf(binding.studyPermitNumber);
            if (Validators.isBlank(licenseNumber) || Validators.isBlank(studyPermitNumber)
                    || licenseExpiryMillis == 0 || permitExpiryMillis == 0) {
                showError(R.string.register_error_required);
                return;
            }
            LicenseClass licenseClass = binding.licenseClass.getSelectedItemPosition() == 0
                    ? LicenseClass.CLASS_4 : LicenseClass.CLASS_5;
            form.setLicenseNumber(licenseNumber);
            form.setLicenseClass(licenseClass);
            form.setLicenseExpiry(licenseExpiryMillis);
            form.setStudyPermitNumber(studyPermitNumber);
            form.setStudyPermitExpiry(permitExpiryMillis);
        }

        if (!binding.termsCheckbox.isChecked()) {
            showError(R.string.register_error_terms);
            return;
        }

        Result<SessionUser> result = viewModel.register(form);
        if (result instanceof Result.Error) {
            showError(getErrorMessage(((Result.Error<SessionUser>) result).code));
            return;
        }

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textOf(EditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void showError(int messageRes) {
        binding.errorBanner.setText(messageRes);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }

    private int getErrorMessage(RepoError code) {
        if (code == UserError.EMAIL_TAKEN) {
            return R.string.register_error_email_taken;
        } else if (code == UserError.LICENSE_NUMBER_TAKEN) {
            return R.string.register_error_license_taken;
        } else if (code == UserError.UNDER_18) {
            return R.string.register_error_under_18;
        } else if (code == UserError.EXPIRY_NOT_FUTURE) {
            return R.string.register_error_expiry;
        } else if (code == UserError.INVALID_INPUT) {
            return R.string.register_error_required;
        }
        return R.string.error_unexpected;
    }
}
