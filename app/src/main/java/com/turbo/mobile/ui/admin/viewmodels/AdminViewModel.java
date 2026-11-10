package com.turbo.mobile.ui.admin.viewmodels;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DocumentKind;
import com.turbo.mobile.data.model.DocumentReviewContext;
import com.turbo.mobile.data.model.PendingDocumentItem;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewOutcome;
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

    public Result<DocumentReviewContext> getDocumentForReview(DocumentKind kind, long documentId) {
        return documentRepository.getDocumentForReview(kind, documentId);
    }

    public Result<ReviewOutcome> approve(DocumentKind kind, long documentId) {
        return documentRepository.approve(sessionManager.getUserId(), kind, documentId);
    }

    public Result<Void> reject(DocumentKind kind, long documentId, String reason) {
        return documentRepository.reject(sessionManager.getUserId(), kind, documentId, reason);
    }

    public int getErrorMessage(RepoError code) {
        if (code == DocumentError.REASON_TOO_SHORT) {
            return R.string.document_review_error_reason;
        } else if (code == DocumentError.NOT_PENDING) {
            return R.string.document_review_error_not_pending;
        } else if (code == DocumentError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == DocumentError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }

    public void logout() {
        sessionManager.clear();
    }
}
