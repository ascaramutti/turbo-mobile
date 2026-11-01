package com.turbo.mobile.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;

/** W-12 Rate & review: after a finished booking, the owner rates the driver and the driver rates the owner. */
public class ReviewActivity extends AppCompatActivity {

    // Names of the Intent extras. The question is ready to show, e.g. "How was Ana L. as a driver?".
    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_QUESTION = "question";

    private AppContainer container;
    private long userId; // the person writing the review
    private long bookingId;
    private RatingBar ratingBar;
    private EditText commentInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TurboUi.setUpScreen(this, R.layout.activity_review);
        TurboUi.showBackButton(this);

        container = ((TurboApp) getApplication()).getContainer();
        userId = container.sessionManager.getUserId();
        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);

        TextView question = findViewById(R.id.rate_question);
        question.setText(getIntent().getStringExtra(EXTRA_QUESTION));
        ratingBar = findViewById(R.id.rating);
        commentInput = findViewById(R.id.input_comment);
        submitButton = findViewById(R.id.submit);

        // "Submit" starts disabled (android:enabled="false") and turns on when a star is tapped.
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> submitButton.setEnabled(rating > 0));
        submitButton.setOnClickListener(v -> submit());
        findViewById(R.id.later).setOnClickListener(v -> finish());
    }

    private void submit() {
        int rating = (int) ratingBar.getRating(); // 1 to 5
        String comment = commentInput.getText().toString().trim();
        submitButton.setEnabled(false); // no double taps while sending
        container.executor.execute(() -> {
            Result<Long> result = container.reviewRepository.submitReview(userId, bookingId, rating, comment);
            runOnUiThread(() -> {
                if (result instanceof Result.Success) {
                    Toast.makeText(this, R.string.review_sent, Toast.LENGTH_SHORT).show();
                    finish(); // back to the previous screen, which reloads in onResume
                } else {
                    TurboUi.showError(this, result);
                    submitButton.setEnabled(true);
                }
            });
        });
    }
}
