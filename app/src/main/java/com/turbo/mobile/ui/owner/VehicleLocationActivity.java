package com.turbo.mobile.ui.owner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.R;

/**
 * W-17 Vehicle location: the pickup address of the vehicle. It's opened from the vehicle form (W-15)
 * and sends the address back to it; the form saves it together with the vehicle.
 */
public class VehicleLocationActivity extends AppCompatActivity {

    // Name of the Intent extra with the address, both when opening this screen and when closing it.
    public static final String EXTRA_ADDRESS = "address";

    private EditText addressInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OwnerUi.setUpScreen(this, R.layout.activity_vehicle_location);
        OwnerUi.showBackButton(this);

        addressInput = findViewById(R.id.input_address);
        addressInput.setText(getIntent().getStringExtra(EXTRA_ADDRESS));
        findViewById(R.id.confirm_location).setOnClickListener(v -> confirm());
    }

    private void confirm() {
        String address = addressInput.getText().toString().trim();
        if (address.isEmpty()) {
            addressInput.setError(getString(R.string.required)); // red mark on the field
            return;
        }
        // Send the address back to the form (its pickLocation launcher receives this Intent).
        Intent result = new Intent();
        result.putExtra(EXTRA_ADDRESS, address);
        setResult(RESULT_OK, result);
        finish();
    }
}
