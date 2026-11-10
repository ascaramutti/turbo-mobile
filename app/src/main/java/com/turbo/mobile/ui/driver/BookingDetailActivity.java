package com.turbo.mobile.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.BookingDetail;
import com.turbo.mobile.data.model.BookingError;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.ConfirmResult;
import com.turbo.mobile.data.model.DriverAction;
import com.turbo.mobile.data.model.OwnerAction;
import com.turbo.mobile.data.model.RepoError;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.ReviewItem;
import com.turbo.mobile.data.model.ServiceType;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.databinding.ActivityBookingDetailBinding;
import com.turbo.mobile.databinding.ItemBookingReviewBinding;
import com.turbo.mobile.ui.DisplayFormat;
import com.turbo.mobile.ui.ScreenSetup;
import com.turbo.mobile.ui.driver.fragments.PaymentDialogFragment;
import com.turbo.mobile.ui.driver.viewmodels.BookingDetailViewModel;
import com.turbo.mobile.ui.driver.viewmodels.TripViewModel;
import com.turbo.mobile.ui.owner.fragments.RejectBookingDialogFragment;
import com.turbo.mobile.ui.owner.viewmodels.OwnerBookingViewModel;
import com.turbo.mobile.util.ImageStorage;

import java.util.List;

public class BookingDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";

    private ActivityBookingDetailBinding binding;
    private BookingDetailViewModel viewModel;
    private OwnerBookingViewModel ownerViewModel;
    private TripViewModel tripViewModel;
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
        binding.topBar.back.setOnClickListener(view -> finish());

        AppContainer appContainer = ((TurboApp) getApplication()).getContainer();
        viewModel = new BookingDetailViewModel(appContainer.bookingRepository, appContainer.reviewRepository, appContainer.sessionManager);
        ownerViewModel = new OwnerBookingViewModel(appContainer.bookingRepository, appContainer.sessionManager);
        tripViewModel = new TripViewModel(appContainer.bookingRepository, appContainer.paymentRepository, appContainer.sessionManager);
        imageStorage = appContainer.imageStorage;

        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, 0);
        binding.bookingTitle.setText(getString(R.string.booking_detail_title, bookingId));
        binding.vehiclePhoto.setClipToOutline(true);
        binding.cancelBooking.setOnClickListener(view -> cancel());
        binding.rejectBooking.setOnClickListener(view -> openRejectDialog());
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
            binding.rejectBooking.setVisibility(View.GONE);
            return;
        }
        BookingDetail booking = ((Result.Success<BookingDetail>) result).data;
        showBooking(booking);
        if (viewModel.isOwner(booking)) {
            showOwnerView(booking);
        } else {
            showNote(booking);
            showActions(booking);
        }
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
            binding.personName.setText(getString(R.string.booking_detail_owner, booking.getOwnerName()));
        } else {
            binding.personName.setText(getString(R.string.booking_detail_owner_rating, booking.getOwnerName(), booking.getCounterpartRatingAvg()));
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
        binding.mainAction.setOnClickListener(null);
        if (actions.contains(DriverAction.PAY)) {
            binding.mainAction.setText(getString(R.string.booking_detail_pay, DisplayFormat.money(booking.getTotalPriceCents())));
            binding.mainAction.setOnClickListener(view -> openPayment(booking.getTotalPriceCents()));
        } else if (actions.contains(DriverAction.START_TRIP)) {
            binding.mainAction.setText(R.string.booking_detail_start_trip);
            binding.mainAction.setOnClickListener(view -> startTrip());
        } else if (actions.contains(DriverAction.END_TRIP)) {
            binding.mainAction.setText(R.string.booking_detail_end_trip);
            binding.mainAction.setOnClickListener(view -> openTripPhotos(true));
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

    private void showOwnerView(BookingDetail booking) {
        if (booking.getCounterpartRatingAvg() == null) {
            binding.personName.setText(getString(R.string.booking_detail_driver, booking.getDriverName()));
        } else {
            binding.personName.setText(getString(R.string.booking_detail_driver_rating, booking.getDriverName(), booking.getCounterpartRatingAvg()));
        }
        showOwnerNote(booking);

        List<OwnerAction> actions = booking.getOwnerActions();
        binding.cancelBooking.setVisibility(View.GONE);
        binding.rejectBooking.setVisibility(actions.contains(OwnerAction.REJECT) ? View.VISIBLE : View.GONE);
        binding.mainAction.setVisibility(View.VISIBLE);
        binding.mainAction.setOnClickListener(null);
        if (actions.contains(OwnerAction.CONFIRM)) {
            binding.mainAction.setText(R.string.booking_detail_confirm);
            binding.mainAction.setOnClickListener(view -> confirm());
        } else if (actions.contains(OwnerAction.REVIEW)) {
            binding.mainAction.setText(R.string.booking_detail_review_driver);
        } else {
            binding.mainAction.setVisibility(View.GONE);
        }
    }

    private void showOwnerNote(BookingDetail booking) {
        switch (booking.getStatus()) {
            case PENDING:
                int overlapping = booking.getOverlappingPendingIds().size();
                if (overlapping == 0) {
                    binding.bookingNote.setText(R.string.booking_detail_owner_pending);
                } else {
                    binding.bookingNote.setText(getResources().getQuantityString(R.plurals.booking_detail_owner_overlapping, overlapping, overlapping));
                }
                break;
            case CONFIRMED:
                if (booking.isPaid()) {
                    binding.bookingNote.setText(getString(R.string.booking_detail_owner_paid, DisplayFormat.money(booking.getOwnerPayoutCents())));
                } else {
                    long deadline = booking.getPayDeadline();
                    binding.bookingNote.setText(getString(R.string.booking_detail_owner_unpaid, DisplayFormat.date(deadline), DisplayFormat.time(deadline)));
                }
                break;
            case REJECTED:
                if (booking.getStatusReason() == null) {
                    binding.bookingNote.setText(R.string.booking_detail_owner_rejected);
                } else {
                    binding.bookingNote.setText(getString(R.string.booking_detail_owner_rejected_reason, booking.getStatusReason()));
                }
                break;
            case IN_PROGRESS:
                if (booking.isOverdue()) {
                    binding.bookingNote.setText(getString(R.string.booking_detail_owner_overdue, DisplayFormat.time(booking.getEnd())));
                } else {
                    showNote(booking);
                }
                break;
            default:
                showNote(booking);
                break;
        }
    }

    private void confirm() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Result<ConfirmResult> result = ownerViewModel.confirm(bookingId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(ownerViewModel.getErrorMessage(((Result.Error<ConfirmResult>) result).code));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        load();
        binding.successBanner.setText(R.string.booking_detail_confirm_done);
        binding.successBanner.setVisibility(View.VISIBLE);
    }

    private void openRejectDialog() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Bundle arguments = new Bundle();
        arguments.putLong(RejectBookingDialogFragment.ARG_BOOKING_ID, bookingId);
        RejectBookingDialogFragment dialog = new RejectBookingDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getSupportFragmentManager(), "reject_booking");
    }

    public void bookingRejected() {
        load();
        binding.successBanner.setText(R.string.booking_detail_reject_done);
        binding.successBanner.setVisibility(View.VISIBLE);
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
        binding.successBanner.setText(R.string.booking_detail_cancel_done);
        binding.successBanner.setVisibility(View.VISIBLE);
    }

    private void openPayment(int total) {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Bundle arguments = new Bundle();
        arguments.putLong(PaymentDialogFragment.ARG_BOOKING_ID, bookingId);
        arguments.putInt(PaymentDialogFragment.ARG_TOTAL, total);

        PaymentDialogFragment dialog = new PaymentDialogFragment();
        dialog.setArguments(arguments);
        dialog.show(getSupportFragmentManager(), "payment");
    }

    private void startTrip() {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Result<Void> result = tripViewModel.canStart(bookingId);
        if (result instanceof Result.Error) {
            binding.errorBanner.setText(tripViewModel.getErrorMessage(((Result.Error<Void>) result).code, false));
            binding.errorBanner.setVisibility(View.VISIBLE);
            return;
        }
        openTripPhotos(false);
    }

    private void openTripPhotos(boolean isReturn) {
        binding.errorBanner.setVisibility(View.GONE);
        binding.successBanner.setVisibility(View.GONE);

        Intent intent = new Intent(this, TripPhotosActivity.class);
        intent.putExtra(TripPhotosActivity.EXTRA_BOOKING_ID, bookingId);
        intent.putExtra(TripPhotosActivity.EXTRA_IS_RETURN, isReturn);
        startActivity(intent);
    }

    public void paymentDone() {
        load();
        binding.successBanner.setText(R.string.booking_detail_paid);
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
