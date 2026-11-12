package com.turbo.mobile.ui.driver.viewmodels;

import android.net.Uri;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.DocumentError;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.DriverDocumentItem;
import com.turbo.mobile.data.model.DriverStatus;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.repository.DocumentRepository;
import com.turbo.mobile.util.SessionManager;

import java.util.Calendar;
import java.util.List;

public class MyDocumentsViewModel {

    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    private final DocumentRepository documentRepository;
    private final SessionManager sessionManager;

    public MyDocumentsViewModel(DocumentRepository documentRepository, SessionManager sessionManager) {
        this.documentRepository = documentRepository;
        this.sessionManager = sessionManager;
    }

    public Result<DriverStatus> loadStatus() {
        return documentRepository.getDriverStatus(sessionManager.getUserId());
    }

    public List<DriverDocumentItem> getDocuments() {
        return documentRepository.getDriverDocuments(sessionManager.getUserId());
    }

    public Result<Void> upload(DocumentType type, Uri image, long expiryDate) {
        return documentRepository.uploadDriverDocument(sessionManager.getUserId(), type, image, expiryDate);
    }

    public long getDaysLeft(long expiryDate) {
        return (long) Math.ceil((expiryDate - System.currentTimeMillis()) / (double) ONE_DAY);
    }

    public long getExpiryDate(int year, int month, int day) {
        Calendar expiry = Calendar.getInstance();
        expiry.clear();
        expiry.set(year, month, 1);
        int lastDayOfMonth = expiry.getActualMaximum(Calendar.DAY_OF_MONTH);
        expiry.set(Calendar.DAY_OF_MONTH, Math.min(day, lastDayOfMonth));
        return expiry.getTimeInMillis();
    }

    public int getErrorMessage(RepoError code) {
        if (code == DocumentError.ACTION_NOT_ALLOWED) {
            return R.string.my_documents_error_approved;
        } else if (code == DocumentError.EXPIRY_NOT_FUTURE) {
            return R.string.my_documents_error_expiry;
        } else if (code == DocumentError.FILE_ERROR) {
            return R.string.my_documents_error_file;
        } else if (code == DocumentError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == DocumentError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
