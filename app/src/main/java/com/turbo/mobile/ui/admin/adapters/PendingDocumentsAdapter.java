package com.turbo.mobile.ui.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.databinding.ItemPendingDocumentBinding;
import com.turbo.mobile.ui.DisplayFormat;

import java.util.ArrayList;
import java.util.List;

public class PendingDocumentsAdapter extends RecyclerView.Adapter<PendingDocumentsAdapter.PendingDocumentViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(PendingDocumentItem item);
    }

    private final OnDocumentClickListener listener;
    private List<PendingDocumentItem> items = new ArrayList<>();

    public PendingDocumentsAdapter(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PendingDocumentItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PendingDocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingDocumentBinding binding = ItemPendingDocumentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PendingDocumentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingDocumentViewHolder holder, int position) {
        PendingDocumentItem item = items.get(position);
        holder.binding.documentSubject.setText(item.getSubjectName());

        Context context = holder.binding.getRoot().getContext();
        String typeName = getDocumentTypeName(context, item.getType());
        String formattedDate = DisplayFormat.date(item.getUploadedAt());
        holder.binding.documentTypeDate.setText(typeName + " · " + formattedDate);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getDocumentTypeName(Context context, DocumentType type) {
        if (type == null) return "";
        switch (type) {
            case DRIVERS_LICENSE:
                return context.getString(R.string.document_type_drivers_license);
            case STUDY_PERMIT:
                return context.getString(R.string.document_type_study_permit);
            case INSURANCE:
                return context.getString(R.string.document_type_insurance);
            case REGISTRATION:
                return context.getString(R.string.document_type_registration);
            default:
                return type.name();
        }
    }

    static class PendingDocumentViewHolder extends RecyclerView.ViewHolder {
        final ItemPendingDocumentBinding binding;

        PendingDocumentViewHolder(ItemPendingDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
