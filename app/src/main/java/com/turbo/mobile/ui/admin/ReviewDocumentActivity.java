package com.turbo.mobile.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.databinding.ActivityReviewDocumentBinding;

public class ReviewDocumentActivity extends AppCompatActivity {

    private ActivityReviewDocumentBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnApprove.setOnClickListener(v ->
                Toast.makeText(this, "Approve document (visual only)", Toast.LENGTH_SHORT).show());

        binding.btnReject.setOnClickListener(v -> binding.overlayReject.setVisibility(View.VISIBLE));

        binding.btnCancel.setOnClickListener(v -> {
            binding.overlayReject.setVisibility(View.GONE);
            binding.etReason.setText("");
        });

        binding.etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasReason = s.toString().trim().length() > 0;
                binding.btnConfirmReject.setEnabled(hasReason);
                binding.txtReasonError.setVisibility(hasReason ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnConfirmReject.setOnClickListener(v -> {
            binding.overlayReject.setVisibility(View.GONE);
            binding.etReason.setText("");
            Toast.makeText(this, "Document rejected (visual only)", Toast.LENGTH_SHORT).show();
        });
    }
}
