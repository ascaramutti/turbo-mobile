package com.turbo.mobile.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.databinding.ItemPendingDocumentBinding;
import com.turbo.mobile.ui.TurboUi;

import java.util.ArrayList;
import java.util.List;

public class PendingDocumentsAdapter extends RecyclerView.Adapter<PendingDocumentsAdapter.PendingViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(PendingDocumentItem document);
    }

    private final List<PendingDocumentItem> documents = new ArrayList<>();
    private final OnDocumentClickListener listener;

    public PendingDocumentsAdapter(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<PendingDocumentItem> documents) {
        this.documents.clear();
        if (documents != null) {
            this.documents.addAll(documents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingDocumentBinding binding = ItemPendingDocumentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PendingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        PendingDocumentItem document = documents.get(position);
        Context context = holder.binding.getRoot().getContext();
        // The driver's name or the vehicle, e.g. "Honda Fit 2021"
        holder.binding.txtTitle.setText(document.getSubjectName());
        // "Study permit · Uploaded Oct 4"
        holder.binding.txtSubtitle.setText(context.getString(R.string.pendingDocumentSubtitleFormat,
                TurboUi.documentName(context, document.getType()), TurboUi.date(document.getUploadedAt())));
        // The repository doesn't send the vehicle's owner in this list, so that line stays hidden.
        holder.binding.txtOwner.setVisibility(View.GONE);
        StatusChip.apply(holder.binding.chipStatus, context.getString(R.string.chip_pending_review));
        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClick(document);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {
        final ItemPendingDocumentBinding binding;

        PendingViewHolder(ItemPendingDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
