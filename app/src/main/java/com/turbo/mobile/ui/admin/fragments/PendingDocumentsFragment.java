package com.turbo.mobile.ui.admin.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.databinding.FragmentPendingDocumentsBinding;
import com.turbo.mobile.ui.admin.DocumentReviewActivity;
import com.turbo.mobile.ui.admin.adapters.PendingDocumentsAdapter;
import com.turbo.mobile.ui.admin.viewmodels.AdminViewModel;

import java.util.List;

public class PendingDocumentsFragment extends Fragment {

    private FragmentPendingDocumentsBinding binding;
    private AdminViewModel viewModel;
    private PendingDocumentsAdapter adapter;
    private DocumentKind selectedKind = DocumentKind.DRIVER;

    public PendingDocumentsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPendingDocumentsBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new AdminViewModel(appContainer.documentRepository, appContainer.sessionManager);

        binding.recyclerPendingDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PendingDocumentsAdapter(item -> {
            Intent intent = new Intent(requireContext(), DocumentReviewActivity.class);
            intent.putExtra(DocumentReviewActivity.EXTRA_KIND, item.getKind().name());
            intent.putExtra(DocumentReviewActivity.EXTRA_DOCUMENT_ID, item.getDocumentId());
            startActivity(intent);
        });
        binding.recyclerPendingDocuments.setAdapter(adapter);

        binding.tabDrivers.setOnClickListener(v -> selectTab(DocumentKind.DRIVER));
        binding.tabVehicles.setOnClickListener(v -> selectTab(DocumentKind.VEHICLE));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTabCounts();
        loadDocuments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void selectTab(DocumentKind kind) {
        if (selectedKind == kind) return;
        selectedKind = kind;
        loadDocuments();
    }

    private void updateTabCounts() {
        if (binding == null || viewModel == null) return;
        int driversCount = viewModel.getPendingCount(DocumentKind.DRIVER);
        int vehiclesCount = viewModel.getPendingCount(DocumentKind.VEHICLE);

        binding.tabDrivers.setText(getString(R.string.pending_documents_tab_drivers, driversCount));
        binding.tabVehicles.setText(getString(R.string.pending_documents_tab_vehicles, vehiclesCount));
    }

    private void loadDocuments() {
        if (binding == null || viewModel == null) return;

        binding.tabDrivers.setSelected(selectedKind == DocumentKind.DRIVER);
        binding.tabVehicles.setSelected(selectedKind == DocumentKind.VEHICLE);

        List<PendingDocumentItem> items = viewModel.getPending(selectedKind);
        adapter.setItems(items);
    }
}
