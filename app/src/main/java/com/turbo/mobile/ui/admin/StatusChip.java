package com.turbo.mobile.ui.admin;

import android.content.res.ColorStateList;
import android.widget.TextView;

import com.turbo.mobile.R;

/**
 * Applies label, background and text color for a status chip,
 * using a single drawable tinted with the status color.
 */
public final class StatusChip {

    private StatusChip() {
    }

    public static void apply(TextView chip, String status) {
        int background;
        int textColor;

        switch (status) {
            case "Active":
            case "Approved":
            case "Confirmed":
            case "Completed":
            case "Paid":
                background = R.color.turbo_success_bg;
                textColor = R.color.turbo_success;
                break;
            case "Suspended":
            case "Pending review":
            case "Payment pending":
                background = R.color.turbo_warning_bg;
                textColor = R.color.turbo_warning;
                break;
            case "Rejected":
            case "Cancelled":
                background = R.color.turbo_error_bg;
                textColor = R.color.turbo_error;
                break;
            default: // Deactivated, Not paid, unknown
                background = R.color.turbo_neutral_bg;
                textColor = R.color.turbo_text_secondary;
                break;
        }

        chip.setText(status);
        chip.setBackgroundResource(R.drawable.bg_chip);
        chip.setBackgroundTintList(ColorStateList.valueOf(chip.getContext().getColor(background)));
        chip.setTextColor(chip.getContext().getColor(textColor));
    }
}
