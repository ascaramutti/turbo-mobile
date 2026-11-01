package com.turbo.mobile.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.DocumentStatus;
import com.turbo.mobile.data.model.DocumentType;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserProfile;
import com.turbo.mobile.data.model.VehicleCategory;
import com.turbo.mobile.data.model.VehicleServiceType;
import com.turbo.mobile.ui.admin.AllBookingsActivity;
import com.turbo.mobile.ui.admin.PendingDocumentsActivity;
import com.turbo.mobile.ui.admin.UsersActivity;
import com.turbo.mobile.ui.auth.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Code shared by the screens of every role, so each screen doesn't have to repeat it. */
public class TurboUi {

    // Every screen calls this first. It shows the layout and fills the avatar of the top bar.
    // The layout's root view must have android:id="@+id/main" and include view_top_bar.xml.
    public static void setUpScreen(AppCompatActivity activity, int layout) {
        activity.setContentView(layout);
        fitSystemBars(activity);
        showInitials(activity);
    }

    // For the screens without our top bar (login, admin). Call it right after setContentView.
    // The app draws behind the status and navigation bars (edge to edge). The wireframes are
    // light-only, so the bar icons stay dark even when the phone is in dark mode.
    public static void fitSystemBars(AppCompatActivity activity) {
        SystemBarStyle light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(activity, light, light);
        // Padding so nothing ends up under the system bars or the keyboard (ime). The | means "and".
        // While the keyboard is open the screen gets shorter, so the field you type in stays visible.
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // Forgets who is signed in and goes back to the login screen.
    public static void logOut(Activity activity) {
        AppContainer container = ((TurboApp) activity.getApplication()).getContainer();
        container.sessionManager.clear();
        openAsOnlyScreen(activity, LoginActivity.class);
    }

    // The admin bottom bar (Documents, Bookings, Users) and the "Log out" link of the admin screens.
    public static void setUpAdminBar(Activity activity) {
        activity.findViewById(R.id.nav_documents).setOnClickListener(v -> openAsOnlyScreen(activity, PendingDocumentsActivity.class));
        activity.findViewById(R.id.nav_bookings).setOnClickListener(v -> openAsOnlyScreen(activity, AllBookingsActivity.class));
        activity.findViewById(R.id.nav_users).setOnClickListener(v -> openAsOnlyScreen(activity, UsersActivity.class));
        View logOutLink = activity.findViewById(R.id.btnLogout);
        if (logOutLink != null) { // "User account" has no "Log out" link
            logOutLink.setOnClickListener(v -> logOut(activity));
        }
    }

    // Opens a screen and closes all the others, so Back can't return to them
    // (e.g. to the previous user's screens after logging out).
    private static void openAsOnlyScreen(Activity activity, Class<?> screen) {
        Intent intent = new Intent(activity, screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    // Shows the back arrow of the top bar. Tapping it closes the screen and goes back to the previous one.
    public static void showBackButton(AppCompatActivity activity) {
        View back = activity.findViewById(R.id.back);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> activity.finish());
    }

    // Puts the user's initials in the top bar avatar, e.g. "ML" for Maria Lee. The profile is read on
    // the background thread (container.executor) and the text is set back on the main thread (runOnUiThread).
    private static void showInitials(AppCompatActivity activity) {
        AppContainer container = ((TurboApp) activity.getApplication()).getContainer();
        long userId = container.sessionManager.getUserId();
        TextView avatar = activity.findViewById(R.id.avatar);
        container.executor.execute(() -> {
            Result<UserProfile> result = container.userRepository.getProfile(userId);
            if (result instanceof Result.Success) {
                UserProfile profile = ((Result.Success<UserProfile>) result).data;
                String initials = profile.getFirstName().substring(0, 1) + profile.getLastName().substring(0, 1);
                activity.runOnUiThread(() -> avatar.setText(initials));
            }
        });
    }

    // For fragments. Only the main thread may change the screen, so background code sends its screen
    // work here. If the user already left the screen there is nothing to update, so it does nothing.
    public static void runOnMainThread(Fragment fragment, Runnable screenCode) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return; // the user already left this screen
        }
        activity.runOnUiThread(() -> {
            if (fragment.isAdded()) { // still on this screen when the main thread gets to it
                screenCode.run();
            }
        });
    }

    // Shows the repository's error message, e.g. "Plate AB1 23C is already registered".
    // Result<?> accepts any Result (Result<Void>, Result<Long>...): here we only need its message.
    public static void showError(Context context, Result<?> result) {
        String message = ((Result.Error<?>) result).message;
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // A chip is a small colored label. All chips share one shape (bg_turbo_chip.xml);
    // only the text and the colors change.
    public static void paintChip(TextView chip, String text, int backgroundColor, int textColor) {
        chip.setText(text);
        chip.setBackgroundTintList(ContextCompat.getColorStateList(chip.getContext(), backgroundColor));
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), textColor));
        chip.setVisibility(View.VISIBLE);
    }

    // The wireframes' confirmation dialog (dialog_confirm.xml): a title, a message and two buttons.
    // onConfirm runs when the orange button is tapped; the white button just closes the dialog.
    public static void showConfirmDialog(Context context, String title, String message,
                                         String cancelText, String confirmText, Runnable onConfirm) {
        AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.setContentView(R.layout.dialog_confirm);
        TextView titleView = dialog.findViewById(R.id.dialog_title);
        TextView messageView = dialog.findViewById(R.id.dialog_message);
        TextView cancel = dialog.findViewById(R.id.dialog_cancel);
        TextView confirm = dialog.findViewById(R.id.dialog_confirm);

        titleView.setText(title);
        messageView.setText(message);
        cancel.setText(cancelText);
        confirm.setText(confirmText);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        dialog.show();

        // Wireframe look: our rounded card instead of the default background, full width, darker backdrop.
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.6f); // 60% dark, as in the wireframe
    }

    // Reading an image file is slow, so it happens on the background thread (container.executor)
    // and the photo is shown on the main thread. The path can be a seed photo or a photo the user picked.
    public static void loadPhoto(Activity activity, ImageView image, String path) {
        AppContainer container = ((TurboApp) activity.getApplication()).getContainer();
        container.executor.execute(() -> {
            Bitmap bitmap = container.imageStorage.load(path, 300);
            if (bitmap != null) {
                activity.runOnUiThread(() -> image.setImageBitmap(bitmap));
            }
        });
    }

    // Shows the photo box on the right of an item_info_card and loads the photo into it.
    public static void showCardPhoto(Activity activity, View card, String path) {
        View thumbnail = card.findViewById(R.id.card_thumbnail);
        thumbnail.setClipToOutline(true); // cuts the photo to the box's rounded corners
        thumbnail.setVisibility(View.VISIBLE);
        loadPhoto(activity, card.findViewById(R.id.card_image), path);
    }

    // "Sedan", "SUV"... The list in strings_owner.xml starts with "Select", so the enum's position is +1.
    public static String categoryName(Context context, VehicleCategory category) {
        return context.getResources().getStringArray(R.array.vehicle_categories)[category.ordinal() + 1];
    }

    // "Taxi & Delivery" or "Delivery only".
    public static String serviceName(Context context, VehicleServiceType service) {
        return context.getResources().getStringArray(R.array.service_types)[service.ordinal() + 1];
    }

    // "Driver’s license", "Study permit", "Insurance certificate" or "Vehicle registration".
    public static String documentName(Context context, DocumentType type) {
        switch (type) {
            case DRIVERS_LICENSE:
                return context.getString(R.string.drivers_license);
            case STUDY_PERMIT:
                return context.getString(R.string.study_permit);
            case INSURANCE:
                return context.getString(R.string.label_insurance);
            default:
                return context.getString(R.string.label_registration);
        }
    }

    // "Pending review", "Approved", "Rejected", or "Not uploaded" when there is no document (null).
    public static String documentStatusName(Context context, DocumentStatus status) {
        if (status == null) {
            return context.getString(R.string.not_uploaded);
        }
        switch (status) {
            case PENDING:
                return context.getString(R.string.chip_pending_review);
            case APPROVED:
                return context.getString(R.string.chip_approved);
            default:
                return context.getString(R.string.chip_rejected);
        }
    }

    // Money is stored in cents: 4000 -> "CAD 40.00", 124000 -> "CAD 1,240.00".
    public static String money(int cents) {
        return String.format(Locale.US, "CAD %,.2f", cents / 100.0);
    }

    // Without cents, for the big totals of W-19: 124050 -> "CAD 1,240". Dividing two ints drops the decimals.
    public static String moneyWithoutCents(int cents) {
        return String.format(Locale.US, "CAD %,d", cents / 100);
    }

    // Dates are stored in milliseconds: -> "Oct 14".
    public static String date(long millis) {
        return new SimpleDateFormat("MMM d", Locale.US).format(new Date(millis));
    }

    // -> "Wed, Oct 14"
    public static String dayAndDate(long millis) {
        return new SimpleDateFormat("EEE, MMM d", Locale.US).format(new Date(millis));
    }

    // With the year, for expiry dates: -> "Aug 31, 2027"
    public static String fullDate(long millis) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(millis));
    }

    // -> "08:00"
    public static String time(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(millis));
    }

    // -> "08:00–12:00"
    public static String timeRange(long start, long end) {
        return time(start) + "–" + time(end);
    }

    // The wireframes often name the vehicle without its year: "Toyota Corolla 2022" -> "Toyota Corolla".
    public static String withoutYear(String vehicleTitle) {
        return vehicleTitle.substring(0, vehicleTitle.lastIndexOf(' '));
    }
}
