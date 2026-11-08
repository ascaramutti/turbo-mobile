package com.turbo.mobile.ui.driver;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewItem;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.databinding.ActivityBookingDetailBinding;
import com.turbo.mobile.databinding.ItemBookingReviewBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.viewmodels.BookingDetailViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.util.List;

public class BookingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private ActivityBookingDetailBinding binding;
    private BookingDetailViewModel viewModel;
    private ImageStorage imageStorage;
    private long bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ScreenSetup.fitSystemBars(this, binding.getRoot());
        ScreenSetup.showInitials(this, binding.topBar.avatar);

        binding.topBar.back.setVisibility(View.VISIBLE);
        binding.topBar.back.setOnClickListener(v -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new BookingDetailViewModel(appContainer.bookingRepository, appContainer.reviewRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        binding.bookingTitle.setText(getString(R.string.booking_detail_title, bookingId));
        binding.vehiclePhoto.setClipToOutline(true);
        binding.cancelBooking.setOnClickListener(v -> cancel());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Result<BookingDetail> result = viewModel.load(bookingId);
        if (result instanceof Result.Error) {
            showError(((Result.Error<BookingDetail>) result).code);
            binding.content.setVisibility(View.GONE);
            binding.mainAction.setVisibility(View.GONE);
            binding.cancelBooking.setVisibility(View.GONE);
            return;
        }
        BookingDetail booking = ((Result.Success<BookingDetail>) result).data;
        showBooking(booking);
        showNote(booking);
        showActions(booking);
        showReviews();
    }

    private void showBooking(BookingDetail booking) {
        BookingStatus status = booking.getStatus();
        boolean closed = status == BookingStatus.REJECTED || status == BookingStatus.CANCELLED;
        binding.steps.setVisibility(closed ? View.GONE : View.VISIBLE);
        binding.stepPending.setSelected(status == BookingStatus.PENDING);
        binding.stepConfirmed.setSelected(status == BookingStatus.CONFIRMED);
        binding.stepInProgress.setSelected(status == BookingStatus.IN_PROGRESS);
        binding.stepCompleted.setSelected(status == BookingStatus.COMPLETED);

        if (booking.getThumbnailPath() != null) {
            binding.vehiclePhoto.setImageBitmap(imageStorage.load(booking.getThumbnailPath(), 400));
        }
        binding.vehicleTitle.setText(booking.getVehicleTitle());
        if (booking.getCounterpartRatingAvg() == null) {
            binding.ownerName.setText(getString(R.string.booking_detail_owner, booking.getOwnerName()));
        } else {
            binding.ownerName.setText(getString(R.string.booking_detail_owner_rating, booking.getOwnerName(), booking.getCounterpartRatingAvg()));
        }
        binding.chipOverdue.setVisibility(booking.isOverdue() ? View.VISIBLE : View.GONE);

        String service = getString(booking.getServiceType() == ServiceType.TAXI ? R.string.booking_detail_taxi : R.string.booking_detail_delivery);
        String day = DisplayFormat.dayAndDate(booking.getStart());
        String timeRange = DisplayFormat.timeRange(booking.getStart(), booking.getEnd());
        binding.bookingWhen.setText(getString(R.string.booking_detail_when, day, timeRange, booking.getHours(), service));
        binding.pickupAddress.setText(booking.getPickupAddress());

        int total = booking.getTotalPriceCents();
        String rate = DisplayFormat.money(total / booking.getHours());
        binding.rental.setText(getString(R.string.booking_detail_rental, booking.getHours(), rate));
        binding.total.setText(DisplayFormat.money(total));
    }

    private void showNote(BookingDetail booking) {
        switch (booking.getStatus()) {
            case PENDING:
                binding.bookingNote.setText(R.string.booking_detail_waiting_owner);
                break;
            case CONFIRMED:
                if (booking.isPaid()) {
                    binding.bookingNote.setText(R.string.booking_detail_ready);
                } else {
                    long deadline = booking.getPayDeadline();
                    binding.bookingNote.setText(getString(R.string.booking_detail_pay_before, DisplayFormat.date(deadline), DisplayFormat.time(deadline)));
                }
                break;
            case IN_PROGRESS:
                String end = DisplayFormat.time(booking.getEnd());
                if (booking.isOverdue()) {
                    binding.bookingNote.setText(getString(R.string.booking_detail_overdue_note, end));
                } else {
                    binding.bookingNote.setText(getString(R.string.booking_detail_trip_ends, end));
                }
                break;
            case COMPLETED:
                binding.bookingNote.setText(getString(R.string.booking_detail_photos, booking.getPickupPhotoCount(), booking.getReturnPhotoCount()));
                break;
            case REJECTED:
                binding.bookingNote.setText(R.string.booking_detail_rejected);
                break;
            case CANCELLED:
                if (StatusReasons.EXPIRED.equals(booking.getStatusReason())) {
                    binding.bookingNote.setText(R.string.booking_detail_expired);
                } else if (booking.getStatusReason() == null) {
                    binding.bookingNote.setText(R.string.booking_detail_cancelled);
                } else {
                    binding.bookingNote.setText(getString(R.string.booking_detail_cancelled_reason, booking.getStatusReason()));
                }
                break;
        }
    }

    private void showActions(BookingDetail booking) {
        List<DriverAction> actions = booking.getDriverActions();
        binding.mainAction.setVisibility(View.VISIBLE);
        if (actions.contains(DriverAction.PAY)) {
            binding.mainAction.setText(getString(R.string.booking_detail_pay, DisplayFormat.money(booking.getTotalPriceCents())));
        } else if (actions.contains(DriverAction.START_TRIP)) {
            binding.mainAction.setText(R.string.booking_detail_start_trip);
        } else if (actions.contains(DriverAction.END_TRIP)) {
            binding.mainAction.setText(R.string.booking_detail_end_trip);
        } else if (actions.contains(DriverAction.REVIEW)) {
            binding.mainAction.setText(R.string.booking_detail_review);
        } else {
            binding.mainAction.setVisibility(View.GONE);
        }
        binding.cancelBooking.setVisibility(actions.contains(DriverAction.CANCEL) ? View.VISIBLE : View.GONE);
    }

    private void showReviews() {
        List<ReviewItem> reviews = viewModel.getReviews(bookingId);
        binding.reviewList.removeAllViews();
        for (ReviewItem review : reviews) {
            ItemBookingReviewBinding item = ItemBookingReviewBinding.inflate(getLayoutInflater(), binding.reviewList, false);
            item.reviewTitle.setText(getString(R.string.booking_detail_review_title, review.getReviewerName(), review.getRating()));
            item.reviewComment.setText(review.getComment());
            boolean noComment = review.getComment() == null || review.getComment().isEmpty();
            item.reviewComment.setVisibility(noComment ? View.GONE : View.VISIBLE);
            binding.reviewList.addView(item.getRoot());
        }
        binding.reviewsSection.setVisibility(reviews.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void cancel() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Result<Void> result = viewModel.cancel(bookingId);
        if (result instanceof Result.Error) {
            showError(((Result.Error<Void>) result).code);
            return;
        }
        load();
        binding.successBanner.setVisibility(View.VISIBLE);
    }

    private void showError(RepoError code) {
        int message = R.string.error_unexpected;
        if (code == BookingError.NOT_CANCELLABLE) {
            message = R.string.booking_detail_error_not_cancellable;
        } else if (code == BookingError.NOT_FOUND) {
            message = R.string.error_not_found;
        } else if (code == BookingError.NOT_ALLOWED) {
            message = R.string.error_not_allowed;
        }
        binding.errorBanner.setText(message);
        binding.errorBanner.setVisibility(View.VISIBLE);
    }
}
