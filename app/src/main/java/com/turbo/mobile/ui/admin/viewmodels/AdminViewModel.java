package com.turbo.mobile.ui.admin.viewmodels;

import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.List;

public class AdminViewModel {

    private final DocumentRepository documentRepository;
    private final SessionManager sessionManager;

    public AdminViewModel(DocumentRepository documentRepository, SessionManager sessionManager) {
        this.documentRepository = documentRepository;
        this.sessionManager = sessionManager;
    }

    public List<PendingDocumentItem> getPending(DocumentKind kind) {
        return documentRepository.getPendingDocuments(kind);
    }

    public int getPendingCount(DocumentKind kind) {
        return documentRepository.getPendingDocuments(kind).size();
    }

    public void logout() {
        sessionManager.clear();
    }
}
