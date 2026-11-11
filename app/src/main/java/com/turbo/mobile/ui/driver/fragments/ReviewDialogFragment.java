package com.turbo.mobile.ui.driver.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.databinding.DialogReviewBinding;
import com.turbo.mobile.ui.driver.BookingDetailActivity;
import com.turbo.mobile.ui.driver.viewmodels.ReviewViewModel;

public class ReviewDialogFragment extends DialogFragment {

    public static final String ARG_BOOKING_ID = "booking_id";
    public static final String ARG_PERSON_NAME = "person_name";
    public static final String ARG_REVIEWING_DRIVER = "reviewing_driver";

    private DialogReviewBinding binding;
    private ReviewViewModel viewModel;
    private TextView[] stars;
    private int rating = 0;

    public ReviewDialogFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DialogReviewBinding.inflate(inflater, container, false);

        AppContainer appContainer = ((TurboApp) requireActivity().getApplication()).getContainer();
        viewModel = new ReviewViewModel(appContainer.reviewRepository, appContainer.sessionManager);

        String personName = requireArguments().getString(ARG_PERSON_NAME);
        if (requireArguments().getBoolean(ARG_REVIEWING_DRIVER)) {
            binding.question.setText(getString(R.string.review_question_driver, personName));
        } else {
            binding.question.setText(getString(R.string.review_question_owner, personName));
        }

        stars = new TextView[]{binding.star1, binding.star2, binding.star3, binding.star4, binding.star5};
        for (int starIndex = 0; starIndex < stars.length; starIndex++) {
            int starNumber = starIndex + 1;
            stars[starIndex].setOnClickListener(view -> chooseRating(starNumber));
        }

        binding.later.setOnClickListener(view -> dismiss());
        binding.submit.setOnClickListener(view -> submit());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        requireDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void chooseRating(int starNumber) {
        rating = starNumber;
        for (int starIndex = 0; starIndex < stars.length; starIndex++) {
            stars[starIndex].setSelected(starIndex < rating);
        }
    }

    private void submit() {
        binding.errorBanner.setVisibility(View.GONE);
        String comment = binding.comment.getText().toString().trim();
        Result<Long> result = viewModel.submitReview(requireArguments().getLong(ARG_BOOKING_ID), rating, comment);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(viewModel.getErrorMessage(((Result.Error<Long>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        ((BookingDetailActivity) requireActivity()).reviewDone();
        dismiss();
    }
}
