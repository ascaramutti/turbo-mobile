package com.turbo.mobile.ui.owner;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.turbo.mobile.AppContainer;
import com.turbo.mobile.R;
import com.turbo.mobile.TurboApp;
import com.turbo.mobile.data.model.Result;
import com.turbo.mobile.data.model.UserProfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Code shared by all the owner screens, so each screen doesn't have to repeat it. */
public class OwnerUi {

    // Every owner screen calls this first. It shows the layout and fills the avatar of the top bar.
    // The layout's root view must have android:id="@+id/main" and include view_top_bar.xml.
    public static void setUpScreen(AppCompatActivity activity, int layout) {
        // The app draws behind the status and navigation bars (edge to edge). The wireframes are
        // light-only, so the bar icons stay dark even when the phone is in dark mode.
        SystemBarStyle light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(activity, light, light);
        activity.setContentView(layout);
        // Padding so nothing ends up under the system bars or the keyboard (ime). The | means "and".
        // While the keyboard is open the screen gets shorter, so the field you type in stays visible.
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        showInitials(activity);
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

    // -> "08:00"
    public static String time(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(millis));
    }

    // The wireframes often name the vehicle without its year: "Toyota Corolla 2022" -> "Toyota Corolla".
    public static String withoutYear(String vehicleTitle) {
        return vehicleTitle.substring(0, vehicleTitle.lastIndexOf(' '));
    }
}
