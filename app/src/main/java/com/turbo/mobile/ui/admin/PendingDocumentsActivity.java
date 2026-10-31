package com.turbo.mobile.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.R;
import com.turbo.mobile.databinding.ActivityPendingDocumentsBinding;

import java.util.ArrayList;
import java.util.List;

public class PendingDocumentsActivity extends AppCompatActivity {

    private ActivityPendingDocumentsBinding binding;
    private PendingDocumentsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingDocumentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new PendingDocumentsAdapter(document ->
                startActivity(new Intent(this, ReviewDocumentActivity.class)));
        binding.recyclerPendingDocuments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPendingDocuments.setAdapter(adapter);

        binding.segmentDrivers.setOnClickListener(v -> showDrivers());
        binding.segmentVehicles.setOnClickListener(v -> showVehicles());

        showDrivers();
    }

    private void showDrivers() {
        selectSegment(binding.segmentDrivers, binding.segmentVehicles);
        List<PendingDocumentsAdapter.PendingDocumentRow> docs = driverDocuments();
        adapter.setDocuments(docs);
        binding.txtCount.setText(getString(R.string.documentsWaitingFormat, docs.size()));
    }

    private void showVehicles() {
        selectSegment(binding.segmentVehicles, binding.segmentDrivers);
        List<PendingDocumentsAdapter.PendingDocumentRow> docs = vehicleDocuments();
        adapter.setDocuments(docs);
        binding.txtCount.setText(getString(R.string.documentsWaitingFormat, docs.size()));
    }

    private void selectSegment(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_segment_selected);
        active.setTextColor(getColor(R.color.turbo_primary));
        inactive.setBackgroundResource(0);
        inactive.setTextColor(getColor(R.color.turbo_text_secondary));
    }

    /** Placeholder lists matching the "Pending documents" screen of the PDF. Replaced by real data later. */
    private List<PendingDocumentsAdapter.PendingDocumentRow> driverDocuments() {
        List<PendingDocumentsAdapter.PendingDocumentRow> docs = new ArrayList<>();
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Ana Lopez", "Study permit · Uploaded Oct 4", null, "Pending review"));
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Kenji Sato", "Driver's license · Uploaded Oct 4", null, "Pending review"));
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Kenji Sato", "Study permit · Uploaded Oct 3", null, "Pending review"));
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Raj Patel", "Driver's license · Uploaded Oct 3", null, "Pending review"));
        return docs;
    }

    private List<PendingDocumentsAdapter.PendingDocumentRow> vehicleDocuments() {
        List<PendingDocumentsAdapter.PendingDocumentRow> docs = new ArrayList<>();
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Honda Fit 2021", "Insurance certificate · Oct 4", "Maria Lee", "Pending review"));
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Ford Escape 2019", "Vehicle registration · Oct 3", "Tom Wu", "Pending review"));
        docs.add(new PendingDocumentsAdapter.PendingDocumentRow(
                "Kia Soul 2020", "Insurance certificate · Oct 2", "Maria Lee", "Pending review"));
        return docs;
    }
}
