package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.databinding.ActivityPendingDocumentsBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.List;

public class PendingDocumentsActivity extends AppCompatActivity {

    private ActivityPendingDocumentsBinding binding;
    private PendingDocumentsAdapter adapter;
    private DocumentKind kind = DocumentKind.DRIVER; // the selected segment: Drivers or Vehicles

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingDocumentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TurboUi.fitSystemBars(this);
        TurboUi.setUpAdminBar(this);

        adapter = new PendingDocumentsAdapter(document -> openDocument(document));
        binding.recyclerPendingDocuments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPendingDocuments.setAdapter(adapter);

        binding.segmentDrivers.setOnClickListener(v -> showDocuments(DocumentKind.DRIVER));
        binding.segmentVehicles.setOnClickListener(v -> showDocuments(DocumentKind.VEHICLE));
    }

    // Also runs when coming back from "Review document", so a reviewed document leaves the list.
    @Override
    protected void onResume() {
        super.onResume();
        showDocuments(kind);
    }

    private void showDocuments(DocumentKind newKind) {
        kind = newKind;
        if (kind == DocumentKind.DRIVER) {
            selectSegment(binding.segmentDrivers, binding.segmentVehicles);
        } else {
            selectSegment(binding.segmentVehicles, binding.segmentDrivers);
        }

        AppContainer container = ((TurboApp) getApplication()).getContainer();
        container.executor.execute(() -> {
            List<PendingDocumentItem> docs = container.documentRepository.getPendingDocuments(newKind);
            runOnUiThread(() -> {
                adapter.setDocuments(docs);
                binding.txtCount.setText(getString(R.string.documentsWaitingFormat, docs.size()));
            });
        });
    }

    private void openDocument(PendingDocumentItem document) {
        Intent intent = new Intent(this, ReviewDocumentActivity.class);
        intent.putExtra(ReviewDocumentActivity.EXTRA_KIND, document.getKind().name());
        intent.putExtra(ReviewDocumentActivity.EXTRA_DOCUMENT_ID, document.getDocumentId());
        startActivity(intent);
    }

    private void selectSegment(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_segment_selected);
        active.setTextColor(getColor(R.color.turbo_primary));
        inactive.setBackgroundResource(0);
        inactive.setTextColor(getColor(R.color.turbo_text_secondary));
    }
}
