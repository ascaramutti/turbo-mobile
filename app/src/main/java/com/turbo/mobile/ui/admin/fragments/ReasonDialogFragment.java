package com.turbo.mobile.ui.admin.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.DialogReasonBinding;
import com.turbo.mobile.ui.admin.DocumentReviewActivity;
import com.turbo.mobile.ui.admin.viewmodels.AdminViewModel;

public class ReasonDialogFragment extends DialogFragment {

    public static final String ARG_KIND = "arg_kind";
    public static final String ARG_DOCUMENT_ID = "arg_document_id";

    private DialogReasonBinding binding;
    private AdminViewModel viewModel;

    public static ReasonDialogFragment newInstance(DocumentKind kind, long documentId) {
        ReasonDialogFragment fragment = new ReasonDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KIND, kind.name());
        args.putLong(ARG_DOCUMENT_ID, documentId);
        fragment.setArguments(args);
        return fragment;
    }

    public ReasonDialogFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogReasonBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new AdminViewModel(appContainer.documentRepository, appContainer.sessionManager);

        binding.cancel.setOnClickListener(v -> dismiss());
        binding.reject.setOnClickListener(v -> reject());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void reject() {
        binding.errorBanner.setVisibility(View.GONE);
        String reason = binding.reason.getText() != null ? binding.reason.getText().toString().trim() : "";
        if (reason.length() < 5) {
            binding.errorBanner.setText(R.string.document_review_error_reason);
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        DocumentKind kind = DocumentKind.valueOf(requireArguments().getString(ARG_KIND));
        long documentId = requireArguments().getLong(ARG_DOCUMENT_ID);

        Result<Void> result = viewModel.reject(kind, documentId, reason);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Void>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }

        if (getActivity() instanceof DocumentReviewActivity) {
            ((DocumentReviewActivity) getActivity()).onDocumentRejected();
        }
        dismiss();
    }
}
