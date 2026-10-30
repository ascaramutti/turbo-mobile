package com.turbo.mobile.ui.driver;

import android.widget.TextView;

import com.turbo.mobile.R;
import com.turbo.mobile.data.model.BookingListItem;
import com.turbo.mobile.data.model.BookingStatus;
import com.turbo.mobile.data.model.StatusReasons;
import com.turbo.mobile.ui.TurboUi;

/** The two chips of a booking in the driver screens (W-08 list and W-09 detail): its status and its payment. */
public class BookingChips {

    // There is no EXPIRED status: an expired request is closed with the reason "Expired".
    public static boolean isExpired(BookingListItem booking) {
        return StatusReasons.EXPIRED.equals(booking.getStatusReason());
    }

    // "Pending", "Confirmed", "In progress", "Completed", "Rejected", "Expired" or "Cancelled", each with its color.
    public static void paintStatus(TextView chip, BookingListItem booking) {
        String text;
        int background;
        int textColor;
        if (isExpired(booking)) {
            text = chip.getContext().getString(R.string.chip_expired);
            background = R.color.turbo_neutral_bg;
            textColor = R.color.turbo_text_secondary;
        } else if (booking.getStatus() == BookingStatus.PENDING) {
            text = chip.getContext().getString(R.string.chip_pending);
            background = R.color.turbo_warning_bg;
            textColor = R.color.turbo_warning;
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            text = chip.getContext().getString(R.string.chip_confirmed);
            background = R.color.turbo_success_bg;
            textColor = R.color.turbo_success;
        } else if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            text = chip.getContext().getString(R.string.chip_in_progress);
            background = R.color.turbo_warning_bg;
            textColor = R.color.turbo_warning;
        } else if (booking.getStatus() == BookingStatus.COMPLETED) {
            text = chip.getContext().getString(R.string.chip_completed);
            background = R.color.turbo_success_bg;
            textColor = R.color.turbo_success;
        } else if (booking.getStatus() == BookingStatus.REJECTED) {
            text = chip.getContext().getString(R.string.chip_rejected);
            background = R.color.turbo_error_bg;
            textColor = R.color.turbo_error;
        } else { // CANCELLED
            text = chip.getContext().getString(R.string.chip_cancelled);
            background = R.color.turbo_neutral_bg;
            textColor = R.color.turbo_text_secondary;
        }
        TurboUi.paintChip(chip, text, background, textColor);
    }

    // "Paid" (green), "Payment pending" (yellow) while a confirmed booking waits for the payment,
    // or "Not paid" (green, as in the wireframe) for a closed booking that was never paid.
    public static void paintPayment(TextView chip, BookingListItem booking) {
        if (booking.isPaid()) {
            TurboUi.paintChip(chip, chip.getContext().getString(R.string.chip_paid),
                    R.color.turbo_success_bg, R.color.turbo_success);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            TurboUi.paintChip(chip, chip.getContext().getString(R.string.chip_payment_pending),
                    R.color.turbo_warning_bg, R.color.turbo_warning);
        } else {
            TurboUi.paintChip(chip, chip.getContext().getString(R.string.chip_not_paid),
                    R.color.turbo_success_bg, R.color.turbo_success);
        }
    }
}
