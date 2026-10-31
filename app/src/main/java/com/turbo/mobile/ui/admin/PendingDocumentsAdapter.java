package com.turbo.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.databinding.ItemPendingDocumentBinding;

import java.util.ArrayList;
import java.util.List;

public class PendingDocumentsAdapter extends RecyclerView.Adapter<PendingDocumentsAdapter.PendingViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(PendingDocumentRow document);
    }

    private final List<PendingDocumentRow> documents = new ArrayList<>();
    private final OnDocumentClickListener listener;

    public PendingDocumentsAdapter(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<PendingDocumentRow> documents) {
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
        PendingDocumentRow document = documents.get(position);
        holder.binding.txtTitle.setText(document.title);
        holder.binding.txtSubtitle.setText(document.subtitle);
        if (document.owner == null || document.owner.isEmpty()) {
            holder.binding.txtOwner.setVisibility(View.GONE);
        } else {
            holder.binding.txtOwner.setVisibility(View.VISIBLE);
            holder.binding.txtOwner.setText(document.owner);
        }
        StatusChip.apply(holder.binding.chipStatus, document.status);
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

    /** Simple display model for a pending document. Replaced by the real model when we connect the data. */
    public static class PendingDocumentRow {
        public final String title;
        public final String subtitle;
        public final String owner;
        public final String status;

        public PendingDocumentRow(String title, String subtitle, String owner, String status) {
            this.title = title;
            this.subtitle = subtitle;
            this.owner = owner;
            this.status = status;
        }
    }
}
