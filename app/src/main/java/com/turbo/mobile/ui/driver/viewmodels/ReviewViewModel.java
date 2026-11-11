package com.turbo.mobile.ui.driver.viewmodels;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewError;
import com.turbo.mobile.data.repository.ReviewRepository;
import com.turbo.mobile.util.SessionManager;

public class ReviewViewModel {

    private final ReviewRepository reviewRepository;
    private final SessionManager sessionManager;

    public ReviewViewModel(ReviewRepository reviewRepository, SessionManager sessionManager) {
        this.reviewRepository = reviewRepository;
        this.sessionManager = sessionManager;
    }

    public Result<Long> submitReview(long bookingId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            return Result.error(ReviewError.INVALID_RATING, "No stars chosen");
        }
        return reviewRepository.submitReview(sessionManager.getUserId(), bookingId, rating, comment);
    }

    public int getErrorMessage(RepoError code) {
        if (code == ReviewError.INVALID_RATING) {
            return R.string.review_error_invalid_rating;
        } else if (code == ReviewError.NOT_COMPLETED) {
            return R.string.review_error_not_completed;
        } else if (code == ReviewError.ALREADY_REVIEWED) {
            return R.string.review_error_already_reviewed;
        } else if (code == ReviewError.NOT_FOUND) {
            return R.string.error_not_found;
        } else if (code == ReviewError.NOT_ALLOWED) {
            return R.string.error_not_allowed;
        }
        return R.string.error_unexpected;
    }
}
